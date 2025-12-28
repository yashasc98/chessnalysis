package com.chessnalysis.service.matchmaking;

import com.chessnalysis.domain.game.TimeControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Background worker for continuous matchmaking.
 * Periodically checks all time control queues and attempts to create matches.
 * Runs in a separate thread pool and is non-blocking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchmakingWorker {

    private final MatchmakingService matchmakingService;

    /**
     * Poll all queues and attempt to create matches.
     * Runs every 500ms (configurable).
     */
    @Scheduled(fixedDelay = 500, initialDelay = 1000)
    public void processMatches() {
        Arrays.stream(TimeControl.values())
                .forEach(this::tryMatchForTimeControl);
    }

    /**
     * Attempt to create matches for a specific time control.
     * Handles multiple pairs if available.
     */
    private void tryMatchForTimeControl(TimeControl timeControl) {
        try {
            while (matchmakingService.getQueueSize(timeControl) >= 2) {
                var match = matchmakingService.tryCreateMatch(timeControl);
                if (match.isEmpty()) {
                    // No more pairs available
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error during matchmaking for {}: ", timeControl, e);
        }
    }
}

