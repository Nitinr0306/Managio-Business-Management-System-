package com.nitin.saas.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_token", columnList = "token"),
        @Index(name = "idx_email_user_id", columnList = "userId"),
        @Index(name = "idx_email_expires", columnList = "expiresAt"),
        @Index(name = "idx_email_used", columnList = "used"),
        @Index(name = "idx_user_created", columnList = "userId, createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID is ~36 chars → keep buffer but not 500
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    private LocalDateTime usedAt;

    @Column(length = 45)
    private String requestIpAddress;

    @Column(length = 255)
    private String requestUserAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    // ───────────────── VALIDATION ─────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return Boolean.FALSE.equals(used) && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}