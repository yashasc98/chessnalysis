package com.chessnalysis.controller.matchmaking;

import com.chessnalysis.domain.game.TimeControl;
import com.chessnalysis.service.matchmaking.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for matchmaking endpoints.
 * Provides HTTP endpoints for entering/leaving matchmaking queue.
 */
@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {

	private final MatchmakingService matchmakingService;

	/**
	 * Get current queue size for a time control.
	 * Useful for UI to show how many players are waiting.
	 */
	@GetMapping("/queue-size")
	public ResponseEntity<QueueSizeResponse> getQueueSize(
		@RequestParam TimeControl timeControl
	) {
		int size = matchmakingService.getQueueSize(timeControl);
		return ResponseEntity.ok(new QueueSizeResponse(timeControl.getDisplayName(), size));
	}

	/**
	 * Response DTO for queue size.
	 */
	public record QueueSizeResponse(String timeControl, int queueSize) {}
}
