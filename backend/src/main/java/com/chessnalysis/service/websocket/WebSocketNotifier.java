package com.chessnalysis.service.websocket;

import com.chessnalysis.domain.game.TimeControl;
import com.chessnalysis.domain.user.User;
import com.chessnalysis.dto.matchmaking.MatchFoundEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending WebSocket notifications to players.
 * Abstracts the messaging layer for clean separation of concerns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotifier {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Notify a player that a match has been found.
	 */
	public void notifyMatchFound(long userId, User opponent, UUID gameId, String color, TimeControl timeControl) {
			MatchFoundEvent event = new MatchFoundEvent(
			gameId,
			opponent.getId(),
			opponent.getUsername(),
			color,
			timeControl,
			Instant.now()
		);

		try {
			messagingTemplate.convertAndSendToUser(
				String.valueOf(userId),
				"/queue/match-found",
				event
			);
			log.debug("Match notification sent: userId={}, gameId={}", userId, gameId);
		} catch (Exception e) {
			log.error("Failed to send match notification: userId={}, gameId={}", userId, gameId, e);
		}
	}

	/**
	 * Notify a player of a game state update (e.g., opponent move, game over).
	 */
	public void notifyGameUpdate(UUID gameId, Object event) {
		String destination = "/topic/game." + gameId;

		try {
			messagingTemplate.convertAndSend(destination, event);
			log.debug("Game update broadcast: gameId={}", gameId);
		} catch (Exception e) {
			log.error("Failed to broadcast game update: gameId={}", gameId, e);
		}
	}

	/**
	 * Notify a player of a queue status update (optional, for polling-based UI).
	 */
	public void notifyQueueStatus(long userId, String status, int position) {
		try {
			messagingTemplate.convertAndSendToUser(
				String.valueOf(userId),
				"/queue/status",
				new QueueStatusUpdate(status, position)
			);
			log.debug("Queue status update sent: userId={}, position={}", userId, position);
		} catch (Exception e) {
			log.error("Failed to send queue status: userId={}", userId, e);
		}
	}

	/**
	 * DTO for queue status updates.
	 */
	public record QueueStatusUpdate(String status, int position) {}
}
