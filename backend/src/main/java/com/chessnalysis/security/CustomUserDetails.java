package com.chessnalysis.security;

import com.chessnalysis.domain.user.User;

/**
 * Custom user details implementation for JWT-based authentication.
 */
public record CustomUserDetails(
        Long userId,
        String username,
        String role,
        User user
) {
}

