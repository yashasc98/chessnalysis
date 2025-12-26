package com.chessnalysis.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user login request.
 */
public record LoginRequest(
	@NotBlank(message = "Username is required")
	String username,

	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
	String password
) {}

