package com.chessnalysis.domain.game;

import lombok.Getter;

/**
 * Represents the color of a chess player (white or black).
 */
@Getter
public enum Color {
    WHITE("white"), BLACK("black");

    private final String chesslibValue;

    Color(String chesslibValue) {
        this.chesslibValue = chesslibValue;
    }

    /**
     * Get the opposite color.
     */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

