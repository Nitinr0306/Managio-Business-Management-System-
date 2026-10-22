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

/** A hashed, single-use token for email verification or password reset. */
@Entity
@Table(name = "one_time_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OneTimeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose;

    @Enumerated(EnumType.STRING)
    private SubjectType subjectType;

    private long subjectId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    public OneTimeToken(TokenPurpose purpose, SubjectType subjectType, long subjectId, String tokenHash,
                 Instant now, Instant expiresAt) {
        this.purpose = purpose;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        usedAt = now;
    }
}
