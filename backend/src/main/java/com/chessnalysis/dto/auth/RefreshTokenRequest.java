package com.chessnalysis.dto.auth;

/**
 * DTO for refresh token request containing refresh token and device ID.
 */
public record RefreshTokenRequest(
        String refreshToken,
        String deviceId
) {
}

