package com.chessnalysis.dto.game;

import com.chessnalysis.domain.game.GameResult;
import com.chessnalysis.service.game.GameClock;

import java.util.List;
import java.util.UUID;

/**
 * WebSocket event: Full game state sync (sent on reconnection).
 */
public record GameStateSyncEvent(
        UUID gameId,
        String state,
        String fen,
        int moveCount,
        GameClock.ClockSnapshot clock,
        GameResult result,
        String resultReason,
        long whitePlayerId,
        long blackPlayerId,
        List<String> moves
) {
}

