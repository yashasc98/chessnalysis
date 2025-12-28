package com.chessnalysis.dao.game;

import com.chessnalysis.domain.game.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisting game sessions.
 * Stores game metadata, FEN snapshots, and results for recovery/history.
 * Board state reconstructed from FEN and moves on load.
 */
@Repository
public interface GameRepository extends JpaRepository<GameSession, UUID> {

    Optional<GameSession> findById(UUID gameId);

    @Query("SELECT gs FROM GameSession gs WHERE gs.whitePlayerId = :playerId OR gs.blackPlayerId = :playerId ORDER BY gs.createdAt DESC LIMIT 20")
    List<GameSession> findRecentGamesByPlayer(Long playerId);

    @Query("SELECT gs FROM GameSession gs WHERE (gs.whitePlayerId = :playerId OR gs.blackPlayerId = :playerId) AND gs.gameState = 'ACTIVE'")
    List<GameSession> findActiveGamesByPlayer(Long playerId);
}

