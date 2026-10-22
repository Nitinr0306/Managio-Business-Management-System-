package com.managio.modules.team.repository;

import com.managio.modules.team.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndBusinessId(long id, long businessId);

    @Query("select i from Invitation i where i.businessId = :businessId and i.acceptedAt is null "
            + "and i.revokedAt is null and i.expiresAt > :now order by i.createdAt desc")
    List<Invitation> findPending(long businessId, Instant now);

    @Query("select i from Invitation i where i.businessId = :businessId and i.email = :email and i.acceptedAt is null "
            + "and i.revokedAt is null and i.expiresAt > :now")
    List<Invitation> findPendingForEmail(long businessId, String email, Instant now);
}
