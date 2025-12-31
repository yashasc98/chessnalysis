package com.chessnalysis.security;

import com.chessnalysis.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;

import java.util.Collection;
import java.util.List;

/**
 * Custom user details implementation for JWT-based authentication.
 * Implements Spring Security's UserDetails so Principal#getName returns the username
 * (required for STOMP user destinations to match /user/<username>/queue/*).
 */
public record CustomUserDetails(
                Long userId,
                String username,
                String role,
                User user
) implements UserDetails, Principal {

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
        }

        @Override
        public String getPassword() {
                return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
                return username;
        }

        @Override
        public String getName() {
                return username;
        }

        @Override
        public boolean isAccountNonExpired() {
                return true;
        }

        @Override
        public boolean isAccountNonLocked() {
                return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
                return true;
        }

        @Override
        public boolean isEnabled() {
                return true;
        }
}

