package com.managio.modules.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A hashed refresh token. Tokens issued from one sign-in share a family so reuse can revoke them together. */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SubjectType subjectType;

    private long subjectId;
    private String tokenHash;
    private UUID familyId;
    private Instant expiresAt;
    private Instant rotatedAt;
    private Instant revokedAt;
    private String userAgent;
    private String ipAddress;
    private Instant createdAt;

    public RefreshToken(SubjectType subjectType, long subjectId, String tokenHash, UUID familyId, Instant now,
                 Instant expiresAt, String userAgent, String ipAddress) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255));
        this.ipAddress = ipAddress;
    }

    public void markRotated(Instant now) {
        rotatedAt = now;
    }
}
