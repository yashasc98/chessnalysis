package com.chessnalysis.controller.game;

import com.chessnalysis.dto.game.GameSyncRequest;
import com.chessnalysis.dto.game.MoveRequest;
import com.chessnalysis.dto.game.ResignRequest;
import com.chessnalysis.security.CustomUserDetails;
import com.chessnalysis.service.game.GameService;
import com.chessnalysis.websocket.notifier.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket handler for game play operations.
 * Clients connect to /ws/game and send messages to /app/game/* endpoints.
 * Game state is validated and moves are applied via GameService.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final WebSocketNotifier webSocketNotifier;

    /**
     * Handle a move request from a player.
     * Message destination: /app/game/{gameId}/move
     * Validates the move and applies it if legal.
     */
    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable String gameId, @Payload MoveRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UUID gameUuid = UUID.fromString(gameId);
            log.debug("Move received: gameId={}, playerId={}, moveUci={}", gameId, userDetails.userId(), request.moveUci());

            gameService.applyMove(gameUuid, request.moveUci(), userDetails.userId());
            log.info("Move successfully applied: gameId={}, moveUci={}", gameId, request.moveUci());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid game ID format: {}", gameId);
            webSocketNotifier.notifyError(userDetails.userId(), "Invalid game ID");
        } catch (Exception e) {
            log.error("Error applying move: gameId={}, playerId={}, error={}", gameId, userDetails.userId(), e.getMessage(), e);
            webSocketNotifier.notifyError(userDetails.userId(), "Failed to apply move: " + e.getMessage());
        }
    }

    /**
     * Handle a resignation request from a player.
     * Message destination: /app/game/{gameId}/resign
     */
    @MessageMapping("/game/{gameId}/resign")
    public void handleResign(@DestinationVariable String gameId, @Payload ResignRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UUID gameUuid = UUID.fromString(gameId);
            log.debug("Resign received: gameId={}, playerId={}", gameId, userDetails.userId());

            gameService.resign(gameUuid, userDetails.userId());
            log.info("Game resigned: gameId={}, playerId={}", gameId, userDetails.userId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid game ID format: {}", gameId);
            webSocketNotifier.notifyError(userDetails.userId(), "Invalid game ID");
        } catch (Exception e) {
            log.error("Error resigning game: gameId={}, playerId={}, error={}", gameId, userDetails.userId(), e.getMessage(), e);
            webSocketNotifier.notifyError(userDetails.userId(), "Failed to resign: " + e.getMessage());
        }
    }

    /**
     * Handle a draw acceptance request.
     * Message destination: /app/game/{gameId}/draw
     */
    @MessageMapping("/game/{gameId}/draw")
    public void handleDraw(@DestinationVariable String gameId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UUID gameUuid = UUID.fromString(gameId);
            log.debug("Draw accepted: gameId={}, playerId={}", gameId, userDetails.userId());

            gameService.acceptDraw(gameUuid);
            log.info("Draw accepted: gameId={}", gameId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid game ID format: {}", gameId);
            webSocketNotifier.notifyError(userDetails.userId(), "Invalid game ID");
        } catch (Exception e) {
            log.error("Error accepting draw: gameId={}, playerId={}, error={}", gameId, userDetails.userId(), e.getMessage(), e);
            webSocketNotifier.notifyError(userDetails.userId(), "Failed to accept draw: " + e.getMessage());
        }
    }

    /**
     * Handle a game state sync request (on reconnection).
     * Message destination: /app/game/{gameId}/sync
     * Sends full game state to the requesting player.
     */
    @MessageMapping("/game/{gameId}/sync")
    public void handleSync(@DestinationVariable String gameId, @Payload GameSyncRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UUID gameUuid = UUID.fromString(gameId);
            log.debug("Sync requested: gameId={}, playerId={}", gameId, userDetails.userId());

            // Get current game snapshot and send to player
            gameService.getGameSnapshot(gameUuid, game -> {
                webSocketNotifier.notifyGameStateSync(userDetails.userId(), gameUuid, game.getState().name(), game.getEngine().getFen(), game.getEngine().getMoveCount(), game.getClock().getSnapshot(), game.getResult(), game.getResultReason());
                return null;
            });

            log.info("Game sync sent: gameId={}, playerId={}", gameId, userDetails.userId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid game ID format: {}", gameId);
            webSocketNotifier.notifyError(userDetails.userId(), "Invalid game ID");
        } catch (Exception e) {
            log.error("Error syncing game: gameId={}, playerId={}, error={}", gameId, userDetails.userId(), e.getMessage(), e);
            webSocketNotifier.notifyError(userDetails.userId(), "Failed to sync: " + e.getMessage());
        }
    }

    /**
     * Handle game start request (when both players are ready).
     * Message destination: /app/game/{gameId}/start
     * Transitions game from PENDING to ACTIVE.
     */
    @MessageMapping("/game/{gameId}/start")
    public void handleStart(@DestinationVariable String gameId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UUID gameUuid = UUID.fromString(gameId);
            log.debug("Start game requested: gameId={}, playerId={}", gameId, userDetails.userId());

            gameService.startGame(gameUuid);
            log.info("Game started: gameId={}", gameId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid game ID format: {}", gameId);
            webSocketNotifier.notifyError(userDetails.userId(), "Invalid game ID");
        } catch (Exception e) {
            log.error("Error starting game: gameId={}, playerId={}, error={}", gameId, userDetails.userId(), e.getMessage(), e);
            webSocketNotifier.notifyError(userDetails.userId(), "Failed to start game: " + e.getMessage());
        }
    }
}

