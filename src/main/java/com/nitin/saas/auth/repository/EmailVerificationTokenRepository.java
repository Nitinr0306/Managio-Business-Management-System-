package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :cutoffDate")
    void deleteExpiredTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(evt) FROM EmailVerificationToken evt WHERE evt.userId = :userId AND evt.used = false AND evt.expiresAt > :now")
    Long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}