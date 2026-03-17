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
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now " +
            "WHERE rt.userId = :userId AND rt.subjectType = :subjectType")
    void revokeAllByUserIdAndSubjectType(@Param("userId") Long userId,
                                         @Param("now") LocalDateTime now,
                                         @Param("subjectType") String subjectType);

    default void revokeAllUserTokens(Long userId) {
        revokeAllByUserIdAndSubjectType(userId, LocalDateTime.now(), "USER");
    }
    default void revokeAllMemberTokens(Long memberId) {
        revokeAllByUserIdAndSubjectType(memberId, LocalDateTime.now(), "MEMBER");
    }

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false")
    Long countActiveTokensByUserId(@Param("userId") Long userId);
}