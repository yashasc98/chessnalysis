package com.chessnalysis.websocket.interceptor;

import com.chessnalysis.dao.user.UserRepository;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.security.CustomUserDetails;
import com.chessnalysis.security.jwt.JwtProvider;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * WebSocket interceptor for JWT authentication on STOMP connections.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    @Nullable
    public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
        if (message == null) {
            return message;
        }

        log.info("WebSocketAuthInterceptor - preSend called with message: {} and messageChannel: {}", message, channel);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Only authenticate on CONNECT command
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                Long userId = jwtProvider.getUserIdFromToken(token);
                String username = jwtProvider.getUsernameFromToken(token);
                String role = jwtProvider.getRoleFromToken(token);

                log.info("WebSocket auth -  userId: {}, username: {}, role: {}", userId, username, role);

                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("WebSocket auth failed - user not found: {}", userId);
                    return null;
                }

                CustomUserDetails userDetails = new CustomUserDetails(userId, username, role, user);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                // Set on accessor (WebSocket session state)
                accessor.setUser(authentication);

                // Store authentication in session attributes so it's available in all subsequent messages
                accessor.getSessionAttributes().put("SPRING_SECURITY_CONTEXT", authentication);

                // Also set SecurityContext for this thread
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("AUTHENTICATION USER SET ON ACCESSOR, SESSION ATTRIBUTES, AND SECURITY CONTEXT");
                log.debug("WebSocket authenticated for user: {}", username);
            } else {
                log.warn("WebSocket connection rejected: invalid or missing token");
                return null;
            }
        }
        // For non-CONNECT messages, WebSocketSecurityContextInterceptor handles restoration

        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

