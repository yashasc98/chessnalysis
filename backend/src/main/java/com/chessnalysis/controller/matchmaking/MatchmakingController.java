package com.chessnalysis.controller.matchmaking;

import com.chessnalysis.domain.game.TimeControl;
import com.chessnalysis.service.matchmaking.MatchmakingService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for matchmaking endpoints.
 * Provides HTTP endpoints for checking queue status.
 */
@Slf4j
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
    public ResponseEntity<QueueSizeResponse> getQueueSize(@RequestParam @Nullable TimeControl timeControl) {
        log.info("Queue-size API called with timeControl: {}", timeControl);
        int size = matchmakingService.getQueueSize(timeControl);
        log.info("Queue size for {}: {}", timeControl, size);
        return ResponseEntity.ok(new QueueSizeResponse(timeControl != null ? timeControl.getDisplayName() : "UNKNOWN", size));
    }

    /**
     * Response DTO for queue size.
     */
    public record QueueSizeResponse(String timeControl, int queueSize) {
    }
}
