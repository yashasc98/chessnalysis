package com.chessnalysis.security;

/**
 * Custom user details implementation for JWT-based authentication.
 */
public record CustomUserDetails(
	Long userId,
	String username,
	String role
) {}

