package com.chessnalysis.dto.auth;

/**
 * DTO for logout request containing device ID.
 */
public record LogoutRequest(
        String deviceId
) {
}

