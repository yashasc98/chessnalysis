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
     */
    public GameClock(TimeControl timeControl) {
        this.timeControl = timeControl;
        long baseMs = (long) timeControl.getBaseSeconds() * 1000;
        this.whiteRemainingMs = new AtomicLong(baseMs);
        this.blackRemainingMs = new AtomicLong(baseMs);
        this.incrementMs = timeControl.getIncrementSeconds() * 1000;
        this.currentPlayer = Color.WHITE;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Get the remaining time for a player in milliseconds.
     */
    public long getRemainingMs(Color color) {
        if (color == Color.WHITE) {
            return whiteRemainingMs.get();
        } else {
            return blackRemainingMs.get();
        }
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
     */
    public java.util.Optional<Color> getTimeoutPlayer() {
        if (whiteRemainingMs.get() <= 0) {
            return java.util.Optional.of(Color.WHITE);
        }
        if (blackRemainingMs.get() <= 0) {
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
        return new ClockSnapshot(
                whiteRemainingMs.get(),
                blackRemainingMs.get(),
                currentPlayer,
                System.currentTimeMillis()
        );
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

