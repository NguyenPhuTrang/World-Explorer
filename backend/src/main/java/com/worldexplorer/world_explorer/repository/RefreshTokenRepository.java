
package com.worldexplorer.world_explorer.repository;

import com.worldexplorer.world_explorer.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Revoke all active refresh tokens belonging to a user
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = CURRENT_TIMESTAMP
            WHERE rt.userId = :userId
              AND rt.revokedAt IS NULL
            """)
    int revokeAllByUserId(@Param("userId") UUID userId);
}
