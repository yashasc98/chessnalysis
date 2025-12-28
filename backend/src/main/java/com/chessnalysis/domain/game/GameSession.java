package com.chessnalysis.domain.game;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a chess game session.
 * Stores game metadata, players, time control, and current state.
 */
@Entity
@Table(name = "game_sessions", indexes = {@Index(name = "idx_game_white_player", columnList = "white_player_id"), @Index(name = "idx_game_black_player", columnList = "black_player_id"), @Index(name = "idx_game_state", columnList = "game_state"), @Index(name = "idx_game_created_at", columnList = "created_at")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Long whitePlayerId;

    @Column(nullable = false)
    private Long blackPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeControl timeControl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GameState gameState = GameState.PENDING;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant lastActivityAt = Instant.now();

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Column(length = 32)
    private String result;

    @Column(length = 256)
    private String resultReason;

    @PreUpdate
    public void preUpdate() {
        this.lastActivityAt = Instant.now();
    }

    public boolean isActive() {
        return gameState == GameState.ACTIVE;
    }

    public boolean isFinished() {
        return gameState == GameState.FINISHED;
    }
}


