package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    default void revokeAllByUserId(Long userId) {
        revokeAllByUserId(userId, LocalDateTime.now());
    }

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoffDate")
    void deleteExpiredTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false")
    Long countActiveTokensByUserId(@Param("userId") Long userId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.deviceId = :deviceId AND rt.userId = :userId AND rt.revoked = false")
    List<RefreshToken> findByDeviceIdAndUserId(@Param("deviceId") String deviceId, @Param("userId") Long userId);
}