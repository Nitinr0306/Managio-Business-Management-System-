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
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :cutoffDate")
    void deleteExpiredTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.userId = :userId AND prt.used = false AND prt.createdAt > :since")
    Long countRecentResetRequests(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}