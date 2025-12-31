package com.chessnalysis.controller.matchmaking;

import com.chessnalysis.dto.matchmaking.EnterQueueRequest;
import com.chessnalysis.dto.matchmaking.LeaveQueueRequest;
import com.chessnalysis.security.CustomUserDetails;
import com.chessnalysis.service.matchmaking.MatchmakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket message handlers for matchmaking messages.
 * Clients send messages to /app/matchmaking/* endpoints.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MatchmakingWebSocketController {

    private final MatchmakingService matchmakingService;

    /**
     * Handle a client's request to enter the matchmaking queue.
     * Message destination: /app/matchmaking/enter
     * Client receives response via /user/queue/match-found when match is created.
     */
    @MessageMapping("/matchmaking/enter")
    public void handleEnterQueue(@Payload EnterQueueRequest request, Message<?> message) {
        try {
            // Extract principal from WebSocket session attributes
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) sessionAttrs.get("SPRING_SECURITY_CONTEXT");
            if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
                log.error("Authentication not found in session attributes or principal is not CustomUserDetails");
                return;
            }

            log.info("Player is trying to enter queue via WebSocket: userId={}, timeControl={}",
                     userDetails.userId(), request.timeControl());

            var queueId = matchmakingService.enterQueue(userDetails.userId(), request.timeControl());

            var position = matchmakingService.getQueuePosition(userDetails.userId(), request.timeControl()).orElse(-1);

            log.info("Player entered queue via WebSocket: userId={}, queueId={}, position={}, timeControl={}",
                     userDetails.userId(), queueId, position, request.timeControl());
        } catch (Exception e) {
            log.error("Error entering queue: ", e);
        }
    }

    /**
     * Handle a client's request to leave the matchmaking queue.
     * Message destination: /app/matchmaking/leave
     */
    @MessageMapping("/matchmaking/leave")
    public void handleLeaveQueue(@Payload LeaveQueueRequest request, Message<?> message) {
        try {
            // Extract principal from WebSocket session attributes
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) sessionAttrs.get("SPRING_SECURITY_CONTEXT");
            if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
                log.error("Authentication not found in session attributes or principal is not CustomUserDetails");
                return;
            }

            log.info("Player is trying to leave queue via WebSocket: userId={}", userDetails.userId());

            UUID queueId = request.queueId();
            boolean removed;

            if (queueId == null) {
                // Leave all queues for this user
                removed = matchmakingService.leaveQueue(userDetails.userId());
                log.info("Player left all queues via WebSocket: userId={}", userDetails.userId());
            } else {
                // Leave specific queue
                removed = matchmakingService.leaveQueueByQueueId(queueId, userDetails.userId());
                log.info("Player left queue via WebSocket: userId={}, queueId={}", userDetails.userId(), queueId);
            }

            if (removed) {
                log.info("Player successfully left queue via WebSocket: userId={}, queueId={}", userDetails.userId(), queueId);
            } else {
                log.warn("Player not found or failed to remove queue entry: userId={}, queueId={}", userDetails.userId(), queueId);
            }
        } catch (Exception e) {
            log.error("Error leaving queue: ", e);
        }
    }
}
