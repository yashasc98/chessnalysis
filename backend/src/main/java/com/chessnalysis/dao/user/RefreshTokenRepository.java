package com.chessnalysis.dao.user;

import com.chessnalysis.domain.user.RefreshToken;
import com.chessnalysis.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken entity persistence.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	List<RefreshToken> findByUserAndRevokedFalse(User user);

    Optional<RefreshToken> findByUserAndDeviceId(User user, String deviceId);

	void deleteByExpiresAtBefore(LocalDateTime expiryDate);

	@Query("SELECT rt FROM RefreshToken rt WHERE rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP ORDER BY rt.createdAt DESC")
	List<RefreshToken> findAllValidTokens();
}

