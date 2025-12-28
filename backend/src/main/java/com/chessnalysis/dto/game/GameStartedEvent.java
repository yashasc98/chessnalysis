package com.chessnalysis.dto.game;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket event: Game started and ready to play.
 */
public record GameStartedEvent(
        UUID gameId,
        long whitePlayerId,
        long blackPlayerId,
        String timeControl,
        Instant startedAt
) {
}
