package com.chessnalysis.domain.game;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user's entry in the matchmaking queue.
 * Used for audit and recovery in case of system failures.
 */
@Entity
@Table(name = "match_queue_entries", indexes = {@Index(name = "idx_queue_user_id", columnList = "user_id"), @Index(name = "idx_queue_status", columnList = "status"), @Index(name = "idx_queue_time_control", columnList = "time_control"), @Index(name = "idx_queue_created_at", columnList = "created_at")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID queueId;

    @Column(nullable = false)
    private Long userId;

    @Column
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeControl timeControl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.QUEUED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column
    private UUID matchedGameId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PreUpdate
    public void preUpdate() {
        // Queue entries are immutable after creation, but we may set matchedGameId on match
    }

    public enum QueueStatus {
        QUEUED, MATCHED, CANCELLED
    }
}
