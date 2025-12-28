package com.chessnalysis.dto.game;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for applying a move in a game.
 */
public record MoveRequest(
        @NotBlank(message = "Move UCI cannot be blank")
        String moveUci
) {
}

