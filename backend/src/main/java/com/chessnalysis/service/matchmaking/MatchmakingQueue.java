package com.chessnalysis.service.matchmaking;

import com.chessnalysis.domain.game.TimeControl;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory matchmaking queue for managing players queued for matches.
 * Organized by TimeControl for efficient pairing.
 * Thread-safe using ConcurrentHashMap and ConcurrentLinkedQueue.
 */
@Slf4j
public class MatchmakingQueue {

    private final Map<TimeControl, Queue<QueuedPlayer>> queues = new ConcurrentHashMap<>();

    public MatchmakingQueue() {
        // Initialize queues for all time controls
        Arrays.stream(TimeControl.values())
                .forEach(tc -> queues.put(tc, new ConcurrentLinkedQueue<>()));
        log.info("MatchmakingQueue initialized with {} time controls", queues.size());
    }

    /**
     * Add a player to the queue for a specific time control.
     * Prevents duplicate queue entries for the same user (regardless of device).
     */
    public boolean enqueue(long userId, TimeControl timeControl) {
        Queue<QueuedPlayer> queue = queues.get(timeControl);
        if (queue == null) {
            log.warn("Time control {} not found", timeControl);
            return false;
        }

        // Check for duplicate entry by userId only (one queue entry per user)
        boolean isDuplicate = queue.stream()
                .anyMatch(p -> p.userId() == userId);

        if (isDuplicate) {
            log.debug("Duplicate queue entry prevented for user: {}", userId);
            return false;
        }

        QueuedPlayer player = new QueuedPlayer(userId, timeControl, Instant.now());
        queue.offer(player);
        log.debug("Player enqueued: userId={}, timeControl={}, queueSize={}", userId, timeControl, queue.size());
        return true;
    }

    /**
     * Remove a player from the queue by userId.
     */
    public boolean dequeue(long userId, TimeControl timeControl) {
        Queue<QueuedPlayer> queue = queues.get(timeControl);
        if (queue == null) {
            return false;
        }

        boolean removed = queue.removeIf(p -> p.userId() == userId);
        if (removed) {
            log.debug("Player dequeued: userId={}, timeControl={}, queueSize={}", userId, timeControl, queue.size());
        }
        return removed;
    }

    /**
     * Atomically pop two players from the queue for pairing.
     * Returns a pair if queue size >= 2, otherwise empty Optional.
     */
    public Optional<Pair> popPair(TimeControl timeControl) {
        Queue<QueuedPlayer> queue = queues.get(timeControl);
        int queueSize = queue != null ? queue.size() : 0;
        log.info("popPair called: timeControl={}, queueSize={}", timeControl, queueSize);
        
        if (queue == null || queue.size() < 2) {
            log.debug("Queue too small for pair: timeControl={}, size={}", timeControl, queueSize);
            return Optional.empty();
        }

        QueuedPlayer first = queue.poll();
        QueuedPlayer second = queue.poll();

        if (first != null && second != null) {
            log.info("Pair created successfully: user1={}, user2={}, timeControl={}", 
                     first.userId(), second.userId(), timeControl);
            return Optional.of(new Pair(first, second));
        }

        // Put them back if something went wrong
        if (first != null) queue.offer(first);
        if (second != null) queue.offer(second);
        
        log.warn("Pair creation failed: first={}, second={}", first, second);
        return Optional.empty();
    }

    /**
     * Get the current queue size for a time control.
     */
    public int getQueueSize(TimeControl timeControl) {
        Queue<QueuedPlayer> queue = queues.get(timeControl);
        int size = queue != null ? queue.size() : 0;

        // Log all non-empty queues for debugging
        queues.forEach((tc, q) -> {
            if (!q.isEmpty()) {
                log.debug("Non-empty queue: timeControl={}, size={}", tc, q.size());
            }
        });

        return size;
    }

    /**
     * Get position of a player in queue (for UI feedback). Uses userId only.
     */
    public Optional<Integer> getQueuePosition(long userId, TimeControl timeControl) {
        Queue<QueuedPlayer> queue = queues.get(timeControl);
        if (queue == null) {
            return Optional.empty();
        }

        int position = 0;
        for (QueuedPlayer p : queue) {
            if (p.userId() == userId) {
                return Optional.of(position);
            }
            position++;
        }
        return Optional.empty();
    }

    /**
     * Check if player is queued for a time control (userId only).
     */
    public boolean isQueued(long userId, TimeControl timeControl) {
        Queue<QueuedPlayer> queue = queues.get(timeControl);
        if (queue == null) {
            return false;
        }
        return queue.stream()
                .anyMatch(p -> p.userId() == userId);
    }

    /**
     * Immutable record representing a player in the queue.
     */
    public record QueuedPlayer(
            long userId,
            TimeControl timeControl,
            Instant queuedAt
    ) {
    }

    /**
     * Immutable record representing a matched pair of players.
     */
    public record Pair(
            QueuedPlayer first,
            QueuedPlayer second
    ) {
        public long firstUserId() {
            return first.userId();
        }

        public long secondUserId() {
            return second.userId();
        }

        public TimeControl timeControl() {
            return first.timeControl();
        }
    }
}

