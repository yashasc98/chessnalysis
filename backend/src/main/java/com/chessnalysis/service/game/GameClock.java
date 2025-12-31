package com.chessnalysis.service.game;

import com.chessnalysis.domain.game.Color;
import com.chessnalysis.domain.game.TimeControl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * GameClock manages time for both players in a chess game.
 * Authoritative on the server: clients cannot modify clock state.
 * Uses millisecond precision with atomic operations for thread-safe updates.
 */
@Slf4j
public class GameClock {

    private final TimeControl timeControl;
    private final AtomicLong whiteRemainingMs;
    private final AtomicLong blackRemainingMs;
    private final int incrementMs;

    @Getter
    private volatile Color currentPlayer;
    @Getter
    private volatile long lastUpdateTime;

    /**
     * Create a clock for the given time control.
     * Note: TimeControl values are in MINUTES (standard chess notation),
     * so we multiply by 60 to convert to seconds, then by 1000 for milliseconds.
     */
    public GameClock(TimeControl timeControl) {
        this.timeControl = timeControl;
        long baseMs = (long) timeControl.getBaseSeconds() * 60 * 1000;
        this.whiteRemainingMs = new AtomicLong(baseMs);
        this.blackRemainingMs = new AtomicLong(baseMs);
        this.incrementMs = timeControl.getIncrementSeconds() * 1000;
        this.currentPlayer = Color.WHITE;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Get the remaining time for a player in milliseconds.
     * For the current player, accounts for elapsed time since last move.
     */
    public long getRemainingMs(Color color) {
        AtomicLong remaining = color == Color.WHITE ? whiteRemainingMs : blackRemainingMs;
        long baseTime = remaining.get();

        // If this is the current player, subtract elapsed time
        if (currentPlayer == color) {
            long elapsed = System.currentTimeMillis() - lastUpdateTime;
            return Math.max(0, baseTime - elapsed);
        }

        return baseTime;
    }

    /**
     * Switch turn to the other player and apply increment.
     * This should be called after a valid move is applied.
     */
    public void switchTurn() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdateTime;

        // Decrement current player's time
        AtomicLong currentTime = currentPlayer == Color.WHITE ? whiteRemainingMs : blackRemainingMs;
        long newTime = Math.max(0, currentTime.get() - elapsed + incrementMs);
        currentTime.set(newTime);

        // Switch to other player
        currentPlayer = currentPlayer.opposite();
        lastUpdateTime = now;

        log.debug("Turn switched to {}, remaining: {} ms", currentPlayer, getRemainingMs(currentPlayer));
    }

    /**
     * Get the player who ran out of time, or empty if both have time.
     * Uses dynamic remaining time calculation (not just stored values).
     */
    public java.util.Optional<Color> getTimeoutPlayer() {
        if (getRemainingMs(Color.WHITE) <= 0) {
            return java.util.Optional.of(Color.WHITE);
        }
        if (getRemainingMs(Color.BLACK) <= 0) {
            return java.util.Optional.of(Color.BLACK);
        }
        return java.util.Optional.empty();
    }

    /**
     * Pause the clock (for disconnect/reconnect scenarios).
     * This freezes the last update time so no time is deducted on resume.
     */
    public void pause() {
        lastUpdateTime = System.currentTimeMillis();
        log.debug("Clock paused");
    }

    /**
     * Resume the clock from a paused state.
     */
    public void resume() {
        lastUpdateTime = System.currentTimeMillis();
        log.debug("Clock resumed");
    }

    /**
     * Check if a player is in time trouble (< 1 minute).
     */
    public boolean isInTimeTrouble(Color color) {
        return getRemainingMs(color) < 60_000;
    }

    /**
     * Get clock state as immutable snapshot for WebSocket events.
     */
    public ClockSnapshot getSnapshot() {
        long now = System.currentTimeMillis();
        return new ClockSnapshot(
                calculateRemainingMs(Color.WHITE, now),
                calculateRemainingMs(Color.BLACK, now),
                currentPlayer,
                now
        );
    }

    private long calculateRemainingMs(Color color, long now) {
        AtomicLong remaining = color == Color.WHITE ? whiteRemainingMs : blackRemainingMs;
        if (currentPlayer == color) {
            long elapsed = now - lastUpdateTime;
            return Math.max(0, remaining.get() - elapsed);
        }
        return remaining.get();
    }

    /**
     * Immutable clock state for transmission.
     */
    public record ClockSnapshot(
            long whiteRemainingMs,
            long blackRemainingMs,
            Color currentPlayer,
            long snapshotTime
    ) {
    }
}

