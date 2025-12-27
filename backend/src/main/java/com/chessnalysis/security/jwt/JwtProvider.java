package com.chessnalysis.security.jwt;

import com.chessnalysis.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT provider for generating and validating JWT tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

	@Value("${auth.jwt.secret:${jwt.secret:chess-nalysis-secret-key-min-256-bits-for-hs256}}")
	private String jwtSecret;

    /**
     * -- GETTER --
     *  Get JWT expiration duration in milliseconds.
     */
    @Getter
    @Value("${auth.jwt.expiration:3600000}")
	private long jwtExpiration;

	/**
	 * Generate a JWT token for the given user.
	 */
	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusMillis(jwtExpiration);

		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.subject(String.valueOf(user.getId()))
			.claim("username", user.getUsername())
			.claim("role", user.getRole().name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	/**
	 * Extract user ID from JWT token.
	 */
	public Long getUserIdFromToken(String token) {
		String subject = parseToken(token).getSubject();
		return Long.parseLong(subject);
	}

	/**
	 * Extract username from JWT token.
	 */
	public String getUsernameFromToken(String token) {
		return parseToken(token).get("username", String.class);
	}

	/**
	 * Extract role from JWT token.
	 */
	public String getRoleFromToken(String token) {
		return parseToken(token).get("role", String.class);
	}

	/**
	 * Validate JWT token.
	 */
	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.warn("JWT token is expired: {}", e.getMessage());
			return false;
		} catch (UnsupportedJwtException e) {
			log.warn("JWT token is unsupported: {}", e.getMessage());
			return false;
		} catch (MalformedJwtException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			return false;
		} catch (SignatureException e) {
			log.warn("JWT signature validation failed: {}", e.getMessage());
			return false;
		} catch (IllegalArgumentException e) {
			log.warn("JWT claims string is empty: {}", e.getMessage());
			return false;
		}
	}

    /**
	 * Parse and return JWT claims.
	 */
	private Claims parseToken(String token) {
		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

}

