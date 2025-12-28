package com.chessnalysis.dto.auth;

/**
 * DTO for login response containing JWT token, refresh token, device ID and user info.
 */
public record LoginResponse(
        String token,
        String refreshToken,
        String deviceId,
        Long userId,
        String username,
        String role,
        Long expiresIn
) {
}

