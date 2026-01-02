package com.chessnalysis.service.game;

import com.chessnalysis.dao.game.GameMoveRepository;
import com.chessnalysis.dao.game.GameRepository;
import com.chessnalysis.domain.game.*;
import com.chessnalysis.exception.ResourceNotFoundException;
import com.chessnalysis.websocket.notifier.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service orchestrating all game play operations.
 * Acts as the primary entry point for game actions (move, resign, draw).
 * Coordinates between GameRegistry (in-memory), GameEngine (logic), and persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRegistry gameRegistry;
    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final WebSocketNotifier webSocketNotifier;

    /**
     * Create a new game from a matchmaking result.
     * Registers the game in memory and persists it.
     */
    @Transactional
    public Game createGame(UUID gameId, long whitePlayerId, long blackPlayerId, TimeControl timeControl) {
        log.info("Creating game: gameId={}, white={}, black={}, timeControl={}", gameId, whitePlayerId, blackPlayerId, timeControl);
        Game game = new Game(gameId, whitePlayerId, blackPlayerId, timeControl);
        gameRegistry.register(game);
        log.info("Game registered in memory: {}", gameId);

        // Auto-start game immediately (clock will start on first move)
        game.start();
        log.info("Game auto-started: {}, state={}", gameId, game.getState());

        // Persist game metadata
        GameSession session = GameSession.builder().id(gameId).whitePlayerId(whitePlayerId).blackPlayerId(blackPlayerId).timeControl(timeControl).gameState(GameState.ACTIVE).currentFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").build();
        gameRepository.save(session);
        log.info("Game persisted to database: {}", gameId);

        // Notify players game is ready
        log.info("About to notify game started for: {}", gameId);
        webSocketNotifier.notifyGameStarted(gameId, whitePlayerId, blackPlayerId, timeControl);
        log.info("Game creation complete: {}", gameId);

        return game;
    }

    /**
     * Start a game (transition PENDING → ACTIVE).
     * Sends GAME_STARTED event to both players.
     */
    @Transactional
    public void startGame(UUID gameId) throws Exception {
        gameRegistry.withWriteLock(gameId, game -> {
            if (game.getState() != GameState.PENDING) {
                throw new IllegalStateException("Game is not PENDING");
            }

            game.start();

            // Update persistence
            GameSession session = gameRepository.findById(gameId).orElseThrow(() -> new ResourceNotFoundException("GameSession not found"));
            session.setGameState(GameState.ACTIVE);
            gameRepository.save(session);

            // Notify players
            webSocketNotifier.notifyGameStarted(gameId, game.getWhitePlayerId(), game.getBlackPlayerId(), game.getTimeControl());

            log.info("Game started: {}", gameId);
        });
    }

    /**
     * Apply a move to a game.
     * Validates turn order, legality, and updates state.
     * Sends MOVE_APPLIED or ILLEGAL_MOVE event.
     */
    @Transactional
    public void applyMove(UUID gameId, String moveUci, long playerId) throws Exception {
        gameRegistry.withWriteLock(gameId, game -> {
            if (!game.applyMove(moveUci, playerId)) {
                // Move was illegal or game ended
                webSocketNotifier.notifyIllegalMove(gameId, moveUci, "Illegal move");
                return;
            }

            // Move was valid
            Color movedColor = game.getEngine().getTurn().opposite(); // Already switched
            String san = "";  // TODO: Implement SAN conversion if needed
            if (!game.getEngine().getMoves().isEmpty()) {
                san = game.getEngine().getMoves().get(game.getEngine().getMoveCount() - 1);
            }

            // Update persistence
            GameSession session = gameRepository.findById(gameId).orElseThrow(() -> new ResourceNotFoundException("GameSession not found"));
            session.setCurrentFen(game.getEngine().getFen());

            if (game.getState() == GameState.FINISHED) {
                session.setGameState(GameState.FINISHED);
                if (game.getResultOptional().isPresent()) {
                    session.setResult(game.getResultOptional().get().name());
                }
                session.setResultReason(game.getResultReason());
            }

            gameRepository.save(session);

            // Save the move to game_moves table
            String fromSquare = moveUci.substring(0, 2);
            String toSquare = moveUci.substring(2, 4);
            GameMove move = GameMove.builder()
                    .gameId(gameId)
                    .moveNumber(game.getEngine().getMoveCount())
                    .fromSquare(fromSquare)
                    .toSquare(toSquare)
                    .moveUci(moveUci)
                    .sanNotation(san)
                    .byPlayerId(playerId)
                    .build();
            gameMoveRepository.save(move);

            // Notify both players
            webSocketNotifier.notifyMoveApplied(gameId, moveUci, san, movedColor, game.getEngine().getMoveCount(), game.getEngine().getFen(), game.getClock().getSnapshot());

            // If game ended, notify
            if (game.getState() == GameState.FINISHED) {
                webSocketNotifier.notifyGameEnded(gameId, game.getResultOptional().orElse(GameResult.DRAW), game.getResultReason(), game.getFinishedAt());
            }

            log.info("Move applied: gameId={}, moveUci={}, san={}", gameId, moveUci, san);
        });
    }

    /**
     * Resign a game.
     */
    @Transactional
    public void resign(UUID gameId, long playerId) throws Exception {
        gameRegistry.withWriteLock(gameId, game -> {
            game.resign(playerId);

            // Update persistence
            GameSession session = gameRepository.findById(gameId).orElseThrow(() -> new ResourceNotFoundException("GameSession not found"));
            session.setGameState(GameState.FINISHED);
            if (game.getResultOptional().isPresent()) {
                session.setResult(game.getResultOptional().get().name());
            }
            session.setResultReason(game.getResultReason());
            gameRepository.save(session);

            // Notify both players
            GameResult result = game.getResultOptional().isPresent() ? game.getResultOptional().get() : GameResult.DRAW;
            webSocketNotifier.notifyGameEnded(gameId, result, game.getResultReason(), game.getFinishedAt());

            log.info("Game resigned: gameId={}, playerId={}", gameId, playerId);
        });
    }

    /**
     * Accept a draw.
     */
    @Transactional
    public void acceptDraw(UUID gameId) throws Exception {
        gameRegistry.withWriteLock(gameId, game -> {
            game.acceptDraw();

            // Update persistence
            GameSession session = gameRepository.findById(gameId).orElseThrow(() -> new ResourceNotFoundException("GameSession not found"));
            session.setGameState(GameState.FINISHED);
            if (game.getResultOptional().isPresent()) {
                session.setResult(game.getResultOptional().get().name());
            }
            session.setResultReason(game.getResultReason());
            gameRepository.save(session);

            // Notify both players
            GameResult result = game.getResultOptional().isPresent() ? game.getResultOptional().get() : GameResult.DRAW;
            webSocketNotifier.notifyGameEnded(gameId, result, game.getResultReason(), game.getFinishedAt());

            log.info("Draw accepted: gameId={}", gameId);
        });
    }

    /**
     * Get a game snapshot for the current state (for sync on reconnection).
     */
    public <T> T getGameSnapshot(UUID gameId, SnapshotHandler<T> handler) throws Exception {
        return gameRegistry.withReadLock(gameId, handler::handle);
    }

    /**
     * Functional interface for snapshot operations.
     */
    @FunctionalInterface
    public interface SnapshotHandler<T> {
        T handle(Game game);
    }
}
