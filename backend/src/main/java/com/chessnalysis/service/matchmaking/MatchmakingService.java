package com.chessnalysis.service.matchmaking;

import com.chessnalysis.dao.game.MatchQueueEntryRepository;
import com.chessnalysis.dao.user.UserRepository;
import com.chessnalysis.domain.game.Game;
import com.chessnalysis.domain.game.MatchQueueEntry;
import com.chessnalysis.domain.game.TimeControl;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.exception.ResourceNotFoundException;
import com.chessnalysis.service.game.GameService;
import com.chessnalysis.websocket.notifier.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Main matchmaking orchestrator.
 * Handles queue management and match creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final MatchmakingQueue matchmakingQueue;
    private final GameService gameService;
    private final MatchQueueEntryRepository matchQueueEntryRepository;
    private final UserRepository userRepository;
    private final WebSocketNotifier webSocketNotifier;

    /**
     * Enqueue a player for matchmaking.
     * Prevents duplicate entries from same user (regardless of device).
     */
    @Transactional
    public UUID enterQueue(long userId, String deviceId, TimeControl timeControl) {
        // Check for duplicate queued entry by user id
        List<MatchQueueEntry> existingEntries = matchQueueEntryRepository.findQueuedEntriesByUser(userId);
        if (!existingEntries.isEmpty()) {
            log.debug("User already queued: userId={}", userId);
            return existingEntries.getFirst().getQueueId();
        }

        // Add to in-memory queue
        boolean enqueued = matchmakingQueue.enqueue(userId, deviceId, timeControl);
        if (!enqueued) {
            throw new IllegalStateException("Failed to enqueue player");
        }

        // Persist queue entry for audit
        UUID queueId = UUID.randomUUID();
        MatchQueueEntry entry = MatchQueueEntry.builder()
                .queueId(queueId)
                .userId(userId)
                .deviceId(deviceId)
                .timeControl(timeControl)
                .status(MatchQueueEntry.QueueStatus.QUEUED)
                .build();

        matchQueueEntryRepository.save(entry);
        log.info("Player entered queue: userId={}, deviceId={}, timeControl={}, queueId={}",
                userId, deviceId, timeControl, queueId);

        return queueId;
    }

    /**
     * Convenience overload for enqueueing only by userId and timeControl (no deviceId required).
     */
    @Transactional
    public UUID enterQueue(long userId, TimeControl timeControl) {
        return enterQueue(userId, null, timeControl);
    }

    /**
     * Dequeue a player from matchmaking by userId (removes user from all queues).
     */
    @Transactional
    public boolean leaveQueue(long userId) {
        boolean removedAny = false;
        for (TimeControl tc : TimeControl.values()) {
            boolean removed = matchmakingQueue.dequeue(userId, tc);
            removedAny = removedAny || removed;
        }

        // Update persisted queue entries for this user to CANCELLED
        List<MatchQueueEntry> queued = matchQueueEntryRepository.findQueuedEntriesByUser(userId);
        queued.forEach(entry -> {
            entry.setStatus(MatchQueueEntry.QueueStatus.CANCELLED);
            matchQueueEntryRepository.save(entry);
        });

        if (removedAny) {
            log.info("User removed from matchmaking queues: userId={}", userId);
        }
        return removedAny;
    }

    /**
     * Dequeue a player by persisted queueId. Ensures user owns the queue entry.
     */
    @Transactional
    public boolean leaveQueueByQueueId(UUID queueId, long userId) {
        Optional<MatchQueueEntry> entryOpt = matchQueueEntryRepository.findByQueueId(queueId);
        if (entryOpt.isEmpty()) return false;

        MatchQueueEntry entry = entryOpt.get();
        if (entry.getUserId() != userId) {
            log.warn("Attempt to remove queue entry by non-owner: userId={}, entryUserId={}", userId, entry.getUserId());
            return false;
        }

        // Remove from in-memory queue
        boolean removed = matchmakingQueue.dequeue(entry.getUserId(), entry.getTimeControl());
        entry.setStatus(MatchQueueEntry.QueueStatus.CANCELLED);
        matchQueueEntryRepository.save(entry);
        log.info("Queue entry cancelled: queueId={}, userId={}", queueId, userId);
        return removed;
    }

    /**
     * Poll for a match for a player (for REST polling, not critical for WebSocket).
     * Returns the Game if a match was found.
     */
    public Optional<Game> pollMatch(long userId, TimeControl timeControl) {
        // Check if player is still in queue
        if (!matchmakingQueue.isQueued(userId, timeControl)) {
            return Optional.empty();
        }

        // Try to create a match (would happen via background worker in production)
        return tryCreateMatch(timeControl);
    }

    /**
     * Attempt to create a match for a time control.
     * Atomically pops two players and creates a Game.
     * This is called by the background worker thread.
     */
    @Transactional
    public Optional<Game> tryCreateMatch(TimeControl timeControl) {
        Optional<MatchmakingQueue.Pair> pair = matchmakingQueue.popPair(timeControl);

        if (pair.isEmpty()) {
            return Optional.empty();
        }

        MatchmakingQueue.Pair p = pair.get();

        try {
            // Generate game ID and create game session
            UUID gameId = UUID.randomUUID();
            Game game = gameService.createGame(gameId, p.firstUserId(), p.secondUserId(), timeControl);

            // Determine colors (random)
            boolean whiteIsFirst = RandomGenerator.getDefault().nextBoolean();
            long whitePlayerId = whiteIsFirst ? p.firstUserId() : p.secondUserId();
            long blackPlayerId = whiteIsFirst ? p.secondUserId() : p.firstUserId();
            String firstPlayerColor = whiteIsFirst ? "WHITE" : "BLACK";
            String secondPlayerColor = whiteIsFirst ? "BLACK" : "WHITE";

            // Update queue entries with matched game
            List<MatchQueueEntry> firstEntries = matchQueueEntryRepository.findQueuedEntriesByUser(p.firstUserId());
            List<MatchQueueEntry> secondEntries = matchQueueEntryRepository.findQueuedEntriesByUser(p.secondUserId());

            firstEntries.forEach(entry -> {
                entry.setStatus(MatchQueueEntry.QueueStatus.MATCHED);
                entry.setMatchedGameId(game.getId());
                matchQueueEntryRepository.save(entry);
            });

            secondEntries.forEach(entry -> {
                entry.setStatus(MatchQueueEntry.QueueStatus.MATCHED);
                entry.setMatchedGameId(game.getId());
                matchQueueEntryRepository.save(entry);
            });

            // Load users for notification
            User firstUser = userRepository.findById(p.firstUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            User secondUser = userRepository.findById(p.secondUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Notify both players via WebSocket
            notifyMatchFound(p.firstUserId(), secondUser, game.getId(), firstPlayerColor, timeControl);
            notifyMatchFound(p.secondUserId(), firstUser, game.getId(), secondPlayerColor, timeControl);

            log.info("Match created: gameId={}, white={}, black={}, timeControl={}",
                    game.getId(), whitePlayerId, blackPlayerId, timeControl);

            return Optional.of(game);
        } catch (Exception e) {
            log.error("Failed to create match for players: {} and {}", p.firstUserId(), p.secondUserId(), e);
            // Optionally re-queue players or mark as failed
            throw e;
        }
    }

    /**
     * Get queue position for a player (for UI feedback).
     */
    public Optional<Integer> getQueuePosition(long userId, TimeControl timeControl) {
        return matchmakingQueue.getQueuePosition(userId, timeControl);
    }

    /**
     * Get queue size for a time control.
     */
    public int getQueueSize(TimeControl timeControl) {
        return matchmakingQueue.getQueueSize(timeControl);
    }

    /**
     * Notify a player of a match found.
     */
    private void notifyMatchFound(long userId, User opponent, UUID gameId, String color, TimeControl timeControl) {
        webSocketNotifier.notifyMatchFound(userId, opponent, gameId, color, timeControl);
    }
}
