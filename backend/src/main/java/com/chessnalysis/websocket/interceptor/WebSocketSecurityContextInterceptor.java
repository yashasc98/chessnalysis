package com.chessnalysis.websocket.interceptor;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Restores the SecurityContext from session attributes for handler method execution.
 * This works across executor thread boundaries because session attributes are preserved
 * by Spring's WebSocket infrastructure even when messages are processed on different threads.
 */
@Slf4j
@Component
public class WebSocketSecurityContextInterceptor implements ChannelInterceptor {

    @Override
    @Nullable
    public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
        if (message == null) {
            return message;
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();

        if (sessionAttrs != null) {
            Object auth = sessionAttrs.get("SPRING_SECURITY_CONTEXT");
            if (auth instanceof UsernamePasswordAuthenticationToken authentication) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Restored authentication from session: {}", authentication.getPrincipal());
            }
        }

        return message;
    }
}


