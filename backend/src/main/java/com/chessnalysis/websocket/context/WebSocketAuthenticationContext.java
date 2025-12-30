package com.chessnalysis.websocket.context;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Thread-local context for storing authentication in WebSocket message handlers.
 * Used to work around SecurityContextHolder's thread-local limitations across executor channels.
 * Uses InheritableThreadLocal to propagate authentication from interceptor thread to handler thread.
 */
public class WebSocketAuthenticationContext {
    private static final ThreadLocal<UsernamePasswordAuthenticationToken> authenticationHolder =
            new InheritableThreadLocal<>();

    public static void setAuthentication(UsernamePasswordAuthenticationToken authentication) {
        authenticationHolder.set(authentication);
    }

    public static UsernamePasswordAuthenticationToken getAuthentication() {
        return authenticationHolder.get();
    }
}

