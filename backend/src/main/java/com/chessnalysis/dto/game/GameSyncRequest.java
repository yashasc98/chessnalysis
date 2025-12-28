package com.chessnalysis.dto.game;

/**
 * Request DTO for syncing game state on reconnection.
 */
public record GameSyncRequest(
        String reason
) {
}

