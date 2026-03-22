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
    @Query("DELETE FROM EmailVerificationToken e WHERE e.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(e) FROM EmailVerificationToken e " +
            "WHERE e.userId = :userId AND e.used = false AND e.expiresAt > :now")
    Long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    Optional<EmailVerificationToken> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}