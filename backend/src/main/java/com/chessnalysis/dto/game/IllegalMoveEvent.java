package com.chessnalysis.dto.game;

import java.util.UUID;

/**
 * WebSocket event: Illegal move attempt.
 */
public record IllegalMoveEvent(
        UUID gameId,
        String moveUci,
        String reason
) {
}
