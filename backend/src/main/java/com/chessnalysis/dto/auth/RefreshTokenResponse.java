package com.chessnalysis.dto.auth;

/**
 * DTO for refresh token response containing new access and refresh tokens.
 */
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String deviceId,
        Long expiresIn
) {
}

