package com.managio.modules.team.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** An invitation to join a business with a role. The token is stored hashed (TEAM-1). */
@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private String email;
    private long roleId;
    private String title;
    private String tokenHash;
    private Long invitedByUserId;
    private Instant expiresAt;
    private Instant acceptedAt;
    private Long acceptedByUserId;
    private Instant revokedAt;
    private Instant createdAt;

    public static Invitation create(long businessId, String email, long roleId, String title, String tokenHash,
                             long invitedByUserId, Instant now, Instant expiresAt) {
        Invitation invitation = new Invitation();
        invitation.businessId = businessId;
        invitation.email = email;
        invitation.roleId = roleId;
        invitation.title = title;
        invitation.tokenHash = tokenHash;
        invitation.invitedByUserId = invitedByUserId;
        invitation.createdAt = now;
        invitation.expiresAt = expiresAt;
        return invitation;
    }

    public boolean isPending(Instant now) {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void renew(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void accept(long userId, Instant now) {
        this.acceptedAt = now;
        this.acceptedByUserId = userId;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }
}
