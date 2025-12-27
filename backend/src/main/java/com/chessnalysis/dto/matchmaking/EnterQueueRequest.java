package com.chessnalysis.dto.matchmaking;

import jakarta.validation.constraints.NotNull;
import com.chessnalysis.domain.game.TimeControl;

/**
 * Request DTO for entering the matchmaking queue.
 */
public record EnterQueueRequest(
	@NotNull TimeControl timeControl
) {}
