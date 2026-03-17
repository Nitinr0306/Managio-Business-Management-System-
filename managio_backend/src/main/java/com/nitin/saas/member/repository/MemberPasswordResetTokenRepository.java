package com.nitin.saas.member.repository;

import com.nitin.saas.member.entity.MemberPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MemberPasswordResetTokenRepository
        extends JpaRepository<MemberPasswordResetToken, Long> {

    Optional<MemberPasswordResetToken> findByToken(String token);

    /** Scheduled cleanup — removes tokens expired more than 1 day ago. */
    @Modifying
    @Query("DELETE FROM MemberPasswordResetToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);

    /** Rate-limiting check — count recent unused reset requests for a member. */
    @Query("SELECT COUNT(t) FROM MemberPasswordResetToken t " +
            "WHERE t.memberId = :memberId AND t.used = false AND t.createdAt > :since")
    Long countRecentByMemberId(
            @Param("memberId") Long memberId,
            @Param("since")    LocalDateTime since);
}