package com.chessnalysis.security.jwt;

import com.chessnalysis.dao.user.UserRepository;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter that validates tokens and sets authentication.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		try {
			String token = extractToken(request);

			if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
				Long userId = jwtProvider.getUserIdFromToken(token);
				String username = jwtProvider.getUsernameFromToken(token);
				String role = jwtProvider.getRoleFromToken(token);

				User user = userRepository.findById(userId).orElse(null);
				if (user == null) {
					log.warn("User not found for userId: {}", userId);
					filterChain.doFilter(request, response);
					return;
				}

				CustomUserDetails userDetails = new CustomUserDetails(userId, username, role, user);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails,
					null,
					Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
				);

				SecurityContextHolder.getContext().setAuthentication(authentication);
				log.debug("JWT token validated for user: {}", username);
			}
		} catch (Exception e) {
			log.error("Failed to set user authentication: {}", e.getMessage());
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Extract JWT token from request header.
	 */
	private String extractToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}
		return null;
	}
}

