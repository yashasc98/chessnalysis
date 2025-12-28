package com.chessnalysis.dto.auth;

/**
 * DTO for registration response.
 */
public record RegisterResponse(
        Long userId,
        String username,
        String message
) {
}

