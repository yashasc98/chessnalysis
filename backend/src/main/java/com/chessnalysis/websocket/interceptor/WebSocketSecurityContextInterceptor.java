package com.chessnalysis.websocket.interceptor;

import com.chessnalysis.websocket.context.WebSocketAuthenticationContext;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Propagates the SecurityContext from session attributes to the current thread
 * for all non-CONNECT messages. This ensures handlers can access authentication.
 * Uses both SecurityContextHolder and a custom ThreadLocal to work across executor channels.
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
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        log.debug("WebSocketSecurityContextInterceptor.preSend: STOMP command={}, sessionAttributes present={}",
                accessor.getCommand(), sessionAttributes != null);

        if (sessionAttributes != null) {
            Object auth = sessionAttributes.get("SPRING_SECURITY_CONTEXT");
            log.debug("Found auth object in session: {} (type: {})",
                    auth != null ? "yes" : "no", auth != null ? auth.getClass().getSimpleName() : "null");

            if (auth instanceof UsernamePasswordAuthenticationToken authentication) {
                // Set in SecurityContextHolder for this thread
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(authentication);

                // Also set in custom ThreadLocal that uses InheritableThreadLocal to cross thread boundaries
                WebSocketAuthenticationContext.setAuthentication(authentication);

                log.info("Restored authentication for STOMP command: {} | Principal: {}",
                        accessor.getCommand(), authentication.getPrincipal());
            } else {
                log.warn("No authentication found in session attributes or wrong type. Auth object: {}", auth);
            }
        } else {
            log.warn("Session attributes are null");
        }

        return message;
    }
}


