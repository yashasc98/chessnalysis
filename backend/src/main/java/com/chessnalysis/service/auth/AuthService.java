package com.chessnalysis.service.auth;

import com.chessnalysis.dao.user.UserRepository;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.domain.user.UserRole;
import com.chessnalysis.dto.auth.LoginRequest;
import com.chessnalysis.dto.auth.LoginResponse;
import com.chessnalysis.dto.auth.RefreshTokenRequest;
import com.chessnalysis.dto.auth.RefreshTokenResponse;
import com.chessnalysis.dto.auth.RegisterRequest;
import com.chessnalysis.dto.auth.RegisterResponse;
import com.chessnalysis.exception.AuthenticationException;
import com.chessnalysis.exception.ResourceAlreadyExistsException;
import com.chessnalysis.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static java.util.function.Predicate.not;

/**
 * Service for handling user authentication, registration, and token management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * Register a new user.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResourceAlreadyExistsException("Username already exists: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                "Registration successful"
        );
    }

    /**
     * Authenticate user and return JWT + refresh token.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }

        String deviceId = Optional.ofNullable(request.deviceId())
                .filter(not(String::isBlank))
                .orElseGet(() -> UUID.randomUUID().toString());

        String accessToken = jwtProvider.generateToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user, deviceId);

        log.info("User logged in successfully: {} on device: {}", user.getUsername(), deviceId);

        return new LoginResponse(
                accessToken,
                refreshToken,
                deviceId,
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                jwtProvider.getJwtExpiration()
        );
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        var refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtProvider.generateToken(user);
        String newRefreshToken = refreshTokenService.rotateRefreshToken(request.refreshToken(), request.deviceId());

        log.info("Access token refreshed for user: {} on device: {}", user.getUsername(), request.deviceId());

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken,
                request.deviceId(),
                jwtProvider.getJwtExpiration()
        );
    }

    /**
     * Logout user from a specific device by revoking its refresh token.
     */
    @Transactional
    public void logout(User user, String deviceId) {
        refreshTokenService.revokeTokenByDevice(user, deviceId);
        log.info("User logged out: {} from device: {}", user.getUsername(), deviceId);
    }
}

