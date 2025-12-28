package com.chessnalysis.websocket.notifier;

import com.chessnalysis.domain.game.Color;
import com.chessnalysis.domain.game.GameResult;
import com.chessnalysis.domain.game.TimeControl;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.dto.game.*;
import com.chessnalysis.dto.matchmaking.MatchFoundEvent;
import com.chessnalysis.service.game.GameClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending WebSocket notifications to players.
 * Abstracts the messaging layer for clean separation of concerns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify a player that a match has been found.
     */
    public void notifyMatchFound(long userId, User opponent, UUID gameId, String color, TimeControl timeControl) {
        MatchFoundEvent event = new MatchFoundEvent(gameId, opponent.getId(), opponent.getUsername(), color, timeControl, Instant.now());

        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/match-found", event);
            log.debug("Match notification sent: userId={}, gameId={}", userId, gameId);
        } catch (Exception e) {
            log.error("Failed to send match notification: userId={}, gameId={}", userId, gameId, e);
        }
    }

    /**
     * Notify a player of a game state update (e.g., opponent move, game over).
     */
    public void notifyGameUpdate(UUID gameId, Object event) {
        String destination = "/topic/game." + gameId;

        try {
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Game update broadcast: gameId={}", gameId);
        } catch (Exception e) {
            log.error("Failed to broadcast game update: gameId={}", gameId, e);
        }
    }

    /**
     * Notify a player of a queue status update (optional, for polling-based UI).
     */
    public void notifyQueueStatus(long userId, String status, int position) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/status", new QueueStatusUpdate(status, position));
            log.debug("Queue status update sent: userId={}, position={}", userId, position);
        } catch (Exception e) {
            log.error("Failed to send queue status: userId={}", userId, e);
        }
    }

    /**
     * Notify both players that a game has started.
     */
    public void notifyGameStarted(UUID gameId, long whitePlayerId, long blackPlayerId, TimeControl timeControl) {
        try {
            var event = new GameStartedEvent(gameId, whitePlayerId, blackPlayerId, timeControl.name(), java.time.Instant.now());
            messagingTemplate.convertAndSend("/topic/game." + gameId, event);
            log.debug("Game started event sent: gameId={}", gameId);
        } catch (Exception e) {
            log.error("Failed to send game started event: gameId={}", gameId, e);
        }
    }

    /**
     * Notify both players that a move was applied.
     */
    public void notifyMoveApplied(UUID gameId, String moveUci, String san, Color byColor, int moveNumber, String fen, GameClock.ClockSnapshot clock) {
        try {
            var event = new MoveAppliedEvent(gameId, moveUci, san, byColor, moveNumber, fen, clock);
            messagingTemplate.convertAndSend("/topic/game." + gameId, event);
            log.debug("Move applied event sent: gameId={}, moveUci={}", gameId, moveUci);
        } catch (Exception e) {
            log.error("Failed to send move applied event: gameId={}", gameId, e);
        }
    }

    /**
     * Notify a player that their move was illegal.
     */
    public void notifyIllegalMove(UUID gameId, String moveUci, String reason) {
        try {
            var event = new IllegalMoveEvent(gameId, moveUci, reason);
            messagingTemplate.convertAndSend("/topic/game." + gameId, event);
            log.debug("Illegal move event sent: gameId={}, moveUci={}", gameId, moveUci);
        } catch (Exception e) {
            log.error("Failed to send illegal move event: gameId={}", gameId, e);
        }
    }

    /**
     * Notify both players that the game has ended.
     */
    public void notifyGameEnded(UUID gameId, GameResult result, String reason, java.time.Instant finishedAt) {
        try {
            var event = new GameEndedEvent(gameId, result, reason, finishedAt);
            messagingTemplate.convertAndSend("/topic/game." + gameId, event);
            log.debug("Game ended event sent: gameId={}, result={}", gameId, result);
        } catch (Exception e) {
            log.error("Failed to send game ended event: gameId={}", gameId, e);
        }
    }

    /**
     * Send a full game state sync to a player (on reconnection).
     */
    public void notifyGameStateSync(long userId, UUID gameId, String state, String fen, int moveCount, GameClock.ClockSnapshot clock, GameResult result, String resultReason) {
        try {
            var event = new GameStateSyncEvent(gameId, state, fen, moveCount, clock, result, resultReason);
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/game-sync", event);
            log.debug("Game state sync sent: userId={}, gameId={}", userId, gameId);
        } catch (Exception e) {
            log.error("Failed to send game state sync: userId={}, gameId={}", userId, gameId, e);
        }
    }

    /**
     * Send an error message to a specific player.
     */
    public void notifyError(long userId, String errorMessage) {
        try {
            var event = new ErrorMessage(errorMessage);
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/errors", event);
            log.debug("Error message sent: userId={}, message={}", userId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to send error message: userId={}", userId, e);
        }
    }

    /**
     * DTO for error messages.
     */
    public record ErrorMessage(String message) {
    }

    /**
     * DTO for queue status updates.
     */
    public record QueueStatusUpdate(String status, int position) {
    }
}
