package com.chessnalysis.domain.game;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a move in a chess game.
 * Stores move details, timestamps, and move history.
 */
@Entity
@Table(name = "game_moves", indexes = {@Index(name = "idx_move_game_id", columnList = "game_id"), @Index(name = "idx_move_player_id", columnList = "by_player_id"), @Index(name = "idx_move_created_at", columnList = "created_at")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID gameId;

    @Column(nullable = false)
    private Integer moveNumber;

    @Column(nullable = false, length = 4)
    private String fromSquare;

    @Column(nullable = false, length = 4)
    private String toSquare;

    @Column(nullable = false, length = 16)
    private String moveUci;

    @Column(length = 16)
    private String sanNotation;

    @Column(nullable = false)
    private Long byPlayerId;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

