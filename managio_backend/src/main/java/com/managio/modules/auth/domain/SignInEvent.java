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

/** A sign-in attempt, kept for security review. */
@Entity
@Table(name = "sign_in_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignInEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SubjectType subjectType;

    private Long subjectId;
    private String identifier;
    private String outcome;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;

    public SignInEvent(SubjectType subjectType, Long subjectId, String identifier, String outcome, String detail,
                String ipAddress, String userAgent, Instant createdAt) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.identifier = identifier == null ? null : identifier.substring(0, Math.min(identifier.length(), 255));
        this.outcome = outcome;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255));
        this.createdAt = createdAt;
    }
}
