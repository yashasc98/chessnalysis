package com.chessnalysis.dto.game;

import com.chessnalysis.domain.game.GameResult;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket event: Game ended with result.
 */
public record GameEndedEvent(
        UUID gameId,
        GameResult result,
        String reason,
        Instant finishedAt
) {
}
