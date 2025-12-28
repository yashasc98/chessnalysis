package com.chessnalysis.dto.game;

import com.chessnalysis.service.game.GameClock;

import java.util.UUID;

/**
 * WebSocket event: Clock update (periodic, sent every ~100ms for active players).
 */
public record ClockUpdateEvent(
        UUID gameId,
        GameClock.ClockSnapshot clock
) {
}
