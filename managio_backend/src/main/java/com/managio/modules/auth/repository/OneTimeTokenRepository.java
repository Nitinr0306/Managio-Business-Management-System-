package com.managio.modules.auth.repository;

import com.managio.modules.auth.domain.OneTimeToken;
import com.managio.modules.auth.domain.TokenPurpose;
import com.managio.modules.auth.domain.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /** Retires earlier unused tokens so only the newest link for a purpose works. */
    @Modifying
    @Query("update OneTimeToken t set t.usedAt = :now where t.purpose = :purpose and t.subjectType = :type "
            + "and t.subjectId = :subjectId and t.usedAt is null")
    int retireUnused(TokenPurpose purpose, SubjectType type, long subjectId, Instant now);

    @Modifying
    @Query("delete from OneTimeToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
