package com.chessnalysis.service.auth;

import com.chessnalysis.dao.user.RefreshTokenRepository;
import com.chessnalysis.domain.user.RefreshToken;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Service for managing refresh tokens with secure hashing and rotation.
 * Stores hashed tokens in the database for security.
 * Supports device-specific logout and token rotation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.refresh-token.expiration:604800000}")
    private long refreshTokenExpiration;

    @Value("${auth.refresh-token.length:32}")
    private int tokenLength;

    /**
     * Generate a new refresh token for the user on a specific device.
     * Stores the hashed token in the database.
     */
    @Transactional
    public String generateRefreshToken(User user, String deviceId) {
        String rawToken = generateSecureToken();
        String tokenHash = passwordEncoder.encode(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);

        RefreshToken refreshToken = refreshTokenRepository
                .findByUserAndDeviceId(user, deviceId)
                .map(existing -> {
                    existing.setTokenHash(tokenHash);
                    existing.setExpiresAt(expiresAt);
                    existing.setRevoked(false);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .deviceId(deviceId)
                        .tokenHash(tokenHash)
                        .expiresAt(expiresAt)
                        .build()
                );

        refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token generated for user: {} on device: {}", user.getUsername(), refreshToken.getDeviceId());

        return rawToken;
    }

    /**
     * Validate and retrieve the refresh token.
     * Returns the RefreshToken entity if valid.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationException("Refresh token is missing");
        }

        RefreshToken refreshToken = findValidToken(rawToken);

        if (!refreshToken.isValid()) {
            throw new AuthenticationException("Refresh token is invalid or expired");
        }

        return refreshToken;
    }

    /**
     * Rotate refresh token on a specific device.
     * Invalidates the old token and generates a new one.
     */
    @Transactional
    public String rotateRefreshToken(String rawOldToken, String deviceId) {
        RefreshToken oldToken = findValidToken(rawOldToken);

        if (!oldToken.getDeviceId().equals(deviceId)) {
            log.warn("Device mismatch for token rotation. Expected: {}, Got: {}", oldToken.getDeviceId(), deviceId);
            throw new AuthenticationException("Device ID does not match the token");
        }

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newRawToken = generateSecureToken();
        String newTokenHash = passwordEncoder.encode(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .tokenHash(newTokenHash)
                .deviceId(deviceId)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        refreshTokenRepository.save(newToken);
        log.debug("Refresh token rotated for user: {} on device: {}", oldToken.getUser().getUsername(), deviceId);

        return newRawToken;
    }

    /**
     * Revoke a specific device's refresh token (device-specific logout).
     */
    @Transactional
    public void revokeTokenByDevice(User user, String deviceId) {
        refreshTokenRepository.findByUserAndDeviceId(user, deviceId)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
        log.info("Refresh token revoked for user: {} on device: {}", user.getUsername(), deviceId);
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices).
     */
    @Transactional
    public void revokeAllTokens(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .forEach(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
        log.info("All refresh tokens revoked for user: {}", user.getUsername());
    }

    /**
     * Clean up expired tokens from the database.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Expired refresh tokens cleaned up");
    }

    /**
     * Find a valid refresh token by its raw token value.
     * Compares the raw token against stored hashes using password encoder.
     * Only checks non-revoked, non-expired tokens for better performance.
     */
    private RefreshToken findValidToken(String rawToken) {
        List<RefreshToken> validTokens = refreshTokenRepository.findAllValidTokens();

        for (RefreshToken token : validTokens) {
            if (passwordEncoder.matches(rawToken, token.getTokenHash())) {
                return token;
            }
        }

        throw new AuthenticationException("Refresh token not found or invalid");
    }

    /**
     * Generate a cryptographically secure random token.
     */
    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[tokenLength];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

