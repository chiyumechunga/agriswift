package zm.agriswift.identity.internal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zm.agriswift.identity.domain.RefreshToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Fast lookup by token string (supported by idx_refresh_tokens_token).
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Retrieve all active (unrevoked and unexpired) tokens for a user.
     * Useful for concurrent session limits or auditing user devices.
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.userId = :userId AND r.revoked = false AND r.expiresAt > :now")
    List<RefreshToken> findAllActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Revokes all active refresh tokens for a specific user (e.g., on logout,
     * password reset, or when reuse/replay fraud is detected).
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken r 
           SET r.revoked = true, 
               r.revokedAt = :revokedAt 
         WHERE r.userId = :userId 
           AND r.revoked = false
        """)
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    /**
     * Purges expired refresh tokens from the table.
     * Can be invoked by a scheduled task (@Scheduled) to prevent table bloat.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteAllExpiredSince(@Param("now") Instant now);
}