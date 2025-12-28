package com.chessnalysis.dto.matchmaking;

import java.util.UUID;

/**
 * Request DTO for leaving the matchmaking queue.
 * For safety clients can provide queueId, but server will primarily use authenticated userId.
 */
public record LeaveQueueRequest(
        UUID queueId
) {
}
