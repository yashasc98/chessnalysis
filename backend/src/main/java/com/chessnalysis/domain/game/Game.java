package com.chessnalysis.domain.game;

import com.chessnalysis.service.game.GameClock;
import com.chessnalysis.service.game.GameEngine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

/**
 * Game aggregate root representing a complete chess game in-memory state.
 * Encapsulates GameEngine (board logic) and GameClock (time management).
 * Enforces state machine: PENDING → ACTIVE → FINISHED.
 * Single-writer concurrency: each game instance accessed by only one caller at a time.
 */
@Slf4j
@Getter
public class Game {

    private final UUID id;
    private final long whitePlayerId;
    private final long blackPlayerId;
    private final TimeControl timeControl;
    private final GameEngine engine;
    private final GameClock clock;

    private GameState state;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private GameResult result;
    private String resultReason;

    /**
     * Create a new game from a matchmaking result.
     */
    public Game(UUID gameId, long whitePlayerId, long blackPlayerId, TimeControl timeControl) {
        this.id = gameId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.timeControl = timeControl;
        this.engine = new GameEngine();
        this.clock = new GameClock(timeControl);
        this.state = GameState.PENDING;
        this.createdAt = Instant.now();
        log.info("Game created: gameId={}, white={}, black={}, timeControl={}", gameId, whitePlayerId, blackPlayerId, timeControl);
    }

    /**
     * Create a game from persisted state (recovery/resume).
     */
    public Game(UUID gameId, long whitePlayerId, long blackPlayerId, TimeControl timeControl, String fen) {
        this.id = gameId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.timeControl = timeControl;
        this.engine = new GameEngine(fen);
        this.clock = new GameClock(timeControl);
        this.state = GameState.ACTIVE;
        this.createdAt = Instant.now();
        log.info("Game recovered: gameId={}, fen={}", gameId, fen);
    }

    /**
     * Start the game (transition PENDING → ACTIVE).
     */
    public void start() {
        if (state != GameState.PENDING) {
            throw new IllegalStateException("Cannot start a game that is not PENDING");
        }
        this.state = GameState.ACTIVE;
        this.startedAt = Instant.now();
        this.clock.resume();
        log.info("Game started: gameId={}", id);
    }

    /**
     * Apply a move to the game.
     * Validates legality, updates board and clock, checks for game end.
     * Returns true if move was applied, false if illegal or game ended.
     */
    public boolean applyMove(String moveUci, long playerId) {
        if (state != GameState.ACTIVE) {
            log.warn("Cannot apply move to non-active game: gameId={}, state={}", id, state);
            return false;
        }

        // Verify it's the correct player's turn
        Color expectedColor = getColorForPlayer(playerId);
        if (expectedColor == null || engine.getTurn() != expectedColor) {
            log.warn("Wrong player move attempt: gameId={}, playerId={}, expectedColor={}, actualTurn={}", id, playerId, expectedColor, engine.getTurn());
            return false;
        }

        // Apply move
        if (!engine.applyMove(moveUci)) {
            return false;
        }

        // Update clock
        clock.switchTurn();

        // Check for timeout
        if (clock.getTimeoutPlayer().isPresent()) {
            finishGame(getResultFromTimeout(clock.getTimeoutPlayer().get()), "Time expired");
        } else if (engine.isFinished()) {
            // Check if game ended by checkmate, stalemate, etc.
            finishGame(engine.getResult().orElse(GameResult.DRAW), "Game finished");
        }

        return true;
    }

    /**
     * Resign the game by the given player.
     */
    public void resign(long playerId) {
        if (state != GameState.ACTIVE) {
            log.warn("Cannot resign from non-active game: gameId={}", id);
            return;
        }
        Color color = getColorForPlayer(playerId);
        if (color == null) {
            throw new IllegalArgumentException("Player not part of this game");
        }
        GameResult result = engine.resign(color);
        finishGame(result, "Player resigned");
    }

    /**
     * Accept a draw offer.
     */
    public void acceptDraw() {
        if (state != GameState.ACTIVE) {
            log.warn("Cannot accept draw on non-active game: gameId={}", id);
            return;
        }
        GameResult result = engine.acceptDraw();
        finishGame(result, "Draw accepted");
    }

    /**
     * Finish the game (transition ACTIVE → FINISHED).
     */
    private void finishGame(GameResult result, String reason) {
        if (state != GameState.ACTIVE) {
            return;
        }
        this.state = GameState.FINISHED;
        this.result = result;
        this.resultReason = reason;
        this.finishedAt = Instant.now();
        this.clock.pause();
        log.info("Game finished: gameId={}, result={}, reason={}", id, result, reason);
    }

    /**
     * Get the color assigned to a player.
     */
    private Color getColorForPlayer(long playerId) {
        if (playerId == whitePlayerId) {
            return Color.WHITE;
        } else if (playerId == blackPlayerId) {
            return Color.BLACK;
        }
        return null;
    }

    /**
     * Determine game result based on which player timed out.
     */
    private GameResult getResultFromTimeout(Color timeoutColor) {
        return timeoutColor == Color.WHITE ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
    }

    /**
     * Get the opponent for a given player.
     */
    public long getOpponentId(long playerId) {
        if (playerId == whitePlayerId) {
            return blackPlayerId;
        } else if (playerId == blackPlayerId) {
            return whitePlayerId;
        }
        throw new IllegalArgumentException("Player not part of this game");
    }

    /**
     * Get the color for a player.
     */
    public Color getColor(long playerId) {
        Color color = getColorForPlayer(playerId);
        if (color == null) {
            throw new IllegalArgumentException("Player not part of this game");
        }
        return color;
    }

    /**
     * Get the result as an Optional (for safe handling).
     */
    public java.util.Optional<GameResult> getResultOptional() {
        return java.util.Optional.ofNullable(result);
    }
}
