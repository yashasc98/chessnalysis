package com.chessnalysis.dto.game;

import com.chessnalysis.domain.game.Color;
import com.chessnalysis.service.game.GameClock;

import java.util.UUID;

/**
 * WebSocket event: A move was applied to the game.
 */
public record MoveAppliedEvent(
        UUID gameId,
        String moveUci,
        String sanNotation,
        Color byColor,
        long moveNumber,
        String fen,
        GameClock.ClockSnapshot clock
) {
}
