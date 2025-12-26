package com.chessnalysis.dto.auth;

/**
 * DTO for login response containing JWT token and user info.
 */
public record LoginResponse(
	String token,
	Long userId,
	String username,
	String role
) {}

