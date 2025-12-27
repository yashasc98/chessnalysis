package com.chessnalysis.dao.game;

import com.chessnalysis.domain.game.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GameSession entity persistence.
 */
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

	Optional<GameSession> findById(UUID gameId);

	@Query("SELECT gs FROM GameSession gs WHERE gs.whitePlayerId = :playerId OR gs.blackPlayerId = :playerId")
	List<GameSession> findByPlayerId(Long playerId);

	@Query("SELECT gs FROM GameSession gs WHERE (gs.whitePlayerId = :playerId OR gs.blackPlayerId = :playerId) AND gs.gameState = 'ACTIVE'")
	List<GameSession> findActiveGamesByPlayerId(Long playerId);

	@Query("SELECT gs FROM GameSession gs WHERE gs.gameState = 'PENDING' ORDER BY gs.createdAt DESC LIMIT 10")
	List<GameSession> findRecentPendingGames();
}

