package com.chessnalysis.dao.game;

import com.chessnalysis.domain.game.MatchQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MatchQueueEntry entity persistence and audit.
 */
@Repository
public interface MatchQueueEntryRepository extends JpaRepository<MatchQueueEntry, Long> {

	Optional<MatchQueueEntry> findByQueueId(UUID queueId);

	@Query("SELECT mqe FROM MatchQueueEntry mqe WHERE mqe.userId = :userId AND mqe.status = 'QUEUED'")
	List<MatchQueueEntry> findQueuedEntriesByUser(Long userId);

	@Query("SELECT mqe FROM MatchQueueEntry mqe WHERE mqe.userId = :userId AND mqe.deviceId = :deviceId AND mqe.status = 'QUEUED'")
	Optional<MatchQueueEntry> findQueuedEntryByUserAndDevice(Long userId, String deviceId);
}

