package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(p) FROM PasswordResetToken p " +
            "WHERE p.userId = :userId AND p.used = false AND p.createdAt > :since")
    Long countRecentResetRequests(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}