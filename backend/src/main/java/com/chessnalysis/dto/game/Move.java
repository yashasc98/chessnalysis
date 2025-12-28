package com.chessnalysis.dto.game;

import java.io.Serializable;

/**
 * Represents a chess move in UCI notation (e.g., "e2e4", "e7e8q" for promotion).
 * Immutable and validated by the GameEngine.
 */
public record Move(
        String uci,
        String sanNotation,
        long timestamp
) implements Serializable {

    public Move {
        if (uci == null || uci.isBlank()) {
            throw new IllegalArgumentException("UCI notation cannot be null or blank");
        }
    }

    /**
     * Factory for creating a Move from UCI notation.
     */
    public static Move fromUci(String uci, String sanNotation) {
        return new Move(uci, sanNotation, System.currentTimeMillis());
    }
}

