package com.chessnalysis.service.game;

import com.chessnalysis.dao.game.GameMoveRepository;
import com.chessnalysis.dao.game.GameSessionRepository;
import com.chessnalysis.domain.game.GameMove;
import com.chessnalysis.domain.game.GameSession;
import com.chessnalysis.domain.game.GameState;
import com.chessnalysis.domain.game.TimeControl;
import com.chessnalysis.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing chess game sessions and moves.
 * Handles game creation, state management, and move persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

	private final GameSessionRepository gameSessionRepository;
	private final GameMoveRepository gameMoveRepository;

	/**
	 * Create a new game session with two players.
	 * Initializes game to PENDING state with starting FEN.
	 */
	@Transactional
	public GameSession createGame(long whitePlayerId, long blackPlayerId, TimeControl timeControl) {
		UUID gameId = UUID.randomUUID();

		GameSession game = GameSession.builder()
			.id(gameId)
			.whitePlayerId(whitePlayerId)
			.blackPlayerId(blackPlayerId)
			.timeControl(timeControl)
			.gameState(GameState.PENDING)
			.currentFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
			.build();

		GameSession saved = gameSessionRepository.save(game);
		log.info("Game created: gameId={}, whitePlayer={}, blackPlayer={}, timeControl={}",
			gameId, whitePlayerId, blackPlayerId, timeControl);

		return saved;
	}

	/**
	 * Transition a game from PENDING to ACTIVE.
	 */
	@Transactional
	public void startGame(UUID gameId) {
		GameSession game = gameSessionRepository.findById(gameId)
			.orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));

		if (game.getGameState() == GameState.PENDING) {
			game.setGameState(GameState.ACTIVE);
			game.setStartedAt(Instant.now());
			gameSessionRepository.save(game);
			log.info("Game started: gameId={}", gameId);
		}
	}

	/**
	 * Record a move in a game.
	 * Validates that the move is by one of the players.
	 */
	@Transactional
	public void applyMove(UUID gameId, String moveUci, String fromSquare, String toSquare, String sanNotation, long byPlayerId) {
		GameSession game = gameSessionRepository.findById(gameId)
			.orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));

		// Validate player ownership
		if (byPlayerId != game.getWhitePlayerId() && byPlayerId != game.getBlackPlayerId()) {
			log.warn("Unauthorized move attempt: gameId={}, playerId={}", gameId, byPlayerId);
			throw new IllegalArgumentException("Player not part of this game");
		}

		// Get move count to determine move number
		int moveCount = gameMoveRepository.countMovesByGameId(gameId);
		int moveNumber = moveCount + 1;

		GameMove move = GameMove.builder()
			.gameId(gameId)
			.moveNumber(moveNumber)
			.fromSquare(fromSquare)
			.toSquare(toSquare)
			.moveUci(moveUci)
			.sanNotation(sanNotation)
			.byPlayerId(byPlayerId)
			.build();

		gameMoveRepository.save(move);

		// Update game lastActivityAt
		game.setLastActivityAt(Instant.now());
		gameSessionRepository.save(game);

		log.debug("Move recorded: gameId={}, moveNumber={}, by={}", gameId, moveNumber, byPlayerId);
	}

	/**
	 * Finish a game with result and reason.
	 */
	@Transactional
	public void finishGame(UUID gameId, String result, String resultReason) {
		GameSession game = gameSessionRepository.findById(gameId)
			.orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));

		game.setGameState(GameState.FINISHED);
		game.setResult(result);
		game.setResultReason(resultReason);
		gameSessionRepository.save(game);

		log.info("Game finished: gameId={}, result={}, reason={}", gameId, result, resultReason);
	}

	/**
	 * Get a game by ID.
	 */
	@Transactional(readOnly = true)
	public Optional<GameSession> getGame(UUID gameId) {
		return gameSessionRepository.findById(gameId);
	}

	/**
	 * Get all moves in a game in chronological order.
	 */
	@Transactional(readOnly = true)
	public List<GameMove> getGameMoves(UUID gameId) {
		return gameMoveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
	}

	/**
	 * Get active games for a player.
	 */
	@Transactional(readOnly = true)
	public List<GameSession> getActiveGamesByPlayer(long playerId) {
		return gameSessionRepository.findActiveGamesByPlayerId(playerId);
	}

	/**
	 * Update current FEN position for a game (optional optimization for rapid lookup).
	 */
	@Transactional
	public void updateFen(UUID gameId, String fen) {
		GameSession game = gameSessionRepository.findById(gameId)
			.orElseThrow(() -> new ResourceNotFoundException("Game not found: " + gameId));

		game.setCurrentFen(fen);
		gameSessionRepository.save(game);
	}
}

