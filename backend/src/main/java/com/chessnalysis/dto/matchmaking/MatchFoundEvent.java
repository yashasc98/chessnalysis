package com.chessnalysis.dto.matchmaking;

import com.chessnalysis.domain.game.TimeControl;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket event notifying player of a match found.
 */
public record MatchFoundEvent(
	UUID gameId,
	Long opponentId,
	String opponentUsername,
	String color,
	TimeControl timeControl,
	Instant startAt
) {}
