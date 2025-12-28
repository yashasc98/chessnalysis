package com.chessnalysis.dto.game;

/**
 * Request DTO for resigning from a game.
 */
public record ResignRequest(
        String reason
) {
}

