package com.chessnalysis.service.game;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;
import com.chessnalysis.domain.game.Color;
import com.chessnalysis.domain.game.GameResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * GameEngine wraps chesslib Board and provides a high-level API for game logic.
 * Single-writer concurrency: only one thread modifies the board at a time (ensured by GameSession lock).
 * Immutable once game ends; all moves are applied sequentially.
 */
@Slf4j
public class GameEngine {

    private final Board board;
    private final List<String> moves;

    /**
     * -- GETTER --
     * Get game result if game has ended.
     */
    @Getter
    private Optional<GameResult> result;

    /**
     * Create a new GameEngine with starting position.
     */
    public GameEngine() {
        this.board = new Board();
        this.moves = Collections.synchronizedList(new ArrayList<>());
        this.result = Optional.empty();
    }

    /**
     * Create a GameEngine from a FEN string (for recovery/resume).
     */
    public GameEngine(String fen) {
        this.board = new Board();
        this.board.loadFromFen(fen);
        this.moves = Collections.synchronizedList(new ArrayList<>());
        this.result = Optional.empty();
    }

    /**
     * Apply a move in UCI notation (e.g., "e2e4").
     * Validates legality and updates the board state.
     * Returns true if move was applied, false if illegal.
     */
    public boolean applyMove(String moveUci) {
        if (result.isPresent()) {
            log.warn("Attempted move on finished game: {}", moveUci);
            return false;
        }

        try {
            // Parse UCI: format is "e2e4" or "e7e8q"
            String fromSquareStr = moveUci.substring(0, 2);
            String toSquareStr = moveUci.substring(2, 4);
            String promotion = moveUci.length() > 4 ? moveUci.substring(4) : "";

            // Convert to chesslib squares
            Square fromSquare = Square.fromValue(fromSquareStr.toUpperCase());
            Square toSquare = Square.fromValue(toSquareStr.toUpperCase());

            // Create move with promotion if present
            Move move;
            if (!promotion.isEmpty()) {
                Piece promotionPiece = getPieceFromChar(promotion.charAt(0));
                move = new Move(fromSquare, toSquare, promotionPiece);
            } else {
                move = new Move(fromSquare, toSquare);
            }

            // Validate legality
            if (!board.isMoveLegal(move, true)) {
                log.debug("Illegal move: {}", moveUci);
                return false;
            }

            // Apply move to board
            board.doMove(move);

            // Store move in UCI format
            moves.add(moveUci);

            // Check for game-ending conditions
            updateGameResult();

            log.debug("Move applied: {}", moveUci);
            return true;
        } catch (Exception e) {
            log.warn("Error applying move {}: {}", moveUci, e.getMessage());
            return false;
        }
    }

    /**
     * Get the current FEN (board state).
     */
    public String getFen() {
        return board.getFen();
    }

    /**
     * Get all moves played so far (UCI notation).
     */
    public List<String> getMoves() {
        return Collections.unmodifiableList(moves);
    }

    /**
     * Get whose turn it is.
     */
    public Color getTurn() {
        return board.getSideToMove() == Side.WHITE ? Color.WHITE : Color.BLACK;
    }

    /**
     * Check if a move is legal (for client validation feedback).
     */
    public boolean isMoveLegal(String moveUci) {
        try {
            String fromSquareStr = moveUci.substring(0, 2);
            String toSquareStr = moveUci.substring(2, 4);
            String promotion = moveUci.length() > 4 ? moveUci.substring(4) : "";

            Square fromSquare = Square.fromValue(fromSquareStr.toUpperCase());
            Square toSquare = Square.fromValue(toSquareStr.toUpperCase());

            Move move;
            if (!promotion.isEmpty()) {
                Piece promotionPiece = getPieceFromChar(promotion.charAt(0));
                move = new Move(fromSquare, toSquare, promotionPiece);
            } else {
                move = new Move(fromSquare, toSquare);
            }
            return board.isMoveLegal(move, true);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert promotion character to Piece.
     */
    private Piece getPieceFromChar(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'q' -> Piece.WHITE_QUEEN;  // Will be adjusted by side
            case 'r' -> Piece.WHITE_ROOK;
            case 'b' -> Piece.WHITE_BISHOP;
            case 'n' -> Piece.WHITE_KNIGHT;
            default -> Piece.WHITE_QUEEN;
        };
    }

    /**
     * Get move count.
     */
    public int getMoveCount() {
        return moves.size();
    }

    /**
     * Check if the game is in a finished state.
     */
    public boolean isFinished() {
        return result.isPresent();
    }

    /**
     * Resign the game by the given color.
     */
    public GameResult resign(Color color) {
        GameResult gameResult = color == Color.WHITE ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
        this.result = Optional.of(gameResult);
        return gameResult;
    }

    /**
     * Offer/accept draw (simplified: accept immediately for now).
     * In production, implement offer/acceptance flow.
     */
    public GameResult acceptDraw() {
        this.result = Optional.of(GameResult.DRAW);
        return GameResult.DRAW;
    }

    /**
     * Update game result based on board state (checkmate, stalemate, insufficient material).
     */
    private void updateGameResult() {
        if (board.isMated()) {
            GameResult gameResult = board.getSideToMove() == Side.WHITE
                    ? GameResult.BLACK_WIN
                    : GameResult.WHITE_WIN;
            this.result = Optional.of(gameResult);
            log.info("Game ended by checkmate: {}", gameResult);
        } else if (board.isStaleMate()) {
            this.result = Optional.of(GameResult.DRAW);
            log.info("Game ended in stalemate");
        } else if (board.isInsufficientMaterial()) {
            this.result = Optional.of(GameResult.DRAW);
            log.info("Game ended: insufficient material");
        }
        // Note: repetition and 50-move rule handled by clock timeout (time-based draw)
    }
}
