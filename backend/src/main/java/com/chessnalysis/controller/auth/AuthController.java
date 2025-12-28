package com.chessnalysis.controller.auth;

import com.chessnalysis.dto.auth.LoginRequest;
import com.chessnalysis.dto.auth.LoginResponse;
import com.chessnalysis.dto.auth.LogoutRequest;
import com.chessnalysis.dto.auth.LogoutResponse;
import com.chessnalysis.dto.auth.RefreshTokenRequest;
import com.chessnalysis.dto.auth.RefreshTokenResponse;
import com.chessnalysis.dto.auth.RegisterRequest;
import com.chessnalysis.dto.auth.RegisterResponse;
import com.chessnalysis.security.CustomUserDetails;
import com.chessnalysis.service.auth.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login and obtain JWT token and refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout from a specific device by revoking its refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal CustomUserDetails userDetails, @Valid @RequestBody LogoutRequest request) {
        authService.logout(userDetails.user(), request.deviceId());
        return ResponseEntity.ok(new LogoutResponse("Logout successful"));
    }
}
