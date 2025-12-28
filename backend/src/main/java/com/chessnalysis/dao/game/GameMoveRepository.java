package com.chessnalysis.dao.game;

import com.chessnalysis.domain.game.GameMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for GameMove entity persistence.
 */
@Repository
public interface GameMoveRepository extends JpaRepository<GameMove, Long> {

    List<GameMove> findByGameIdOrderByMoveNumberAsc(UUID gameId);

    @Query("SELECT gm FROM GameMove gm WHERE gm.gameId = :gameId ORDER BY gm.moveNumber DESC LIMIT 1")
    List<GameMove> findLastMove(UUID gameId);

    @Query("SELECT COUNT(gm) FROM GameMove gm WHERE gm.gameId = :gameId")
    int countMovesByGameId(UUID gameId);
}

