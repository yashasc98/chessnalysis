package com.chessnalysis.domain.game;

import lombok.Getter;

/**
 * Enumeration of possible game results.
 */
@Getter
public enum GameResult {
    WHITE_WIN("White wins"), BLACK_WIN("Black wins"), DRAW("Draw");

    private final String displayName;

    GameResult(String displayName) {
        this.displayName = displayName;
    }

}

