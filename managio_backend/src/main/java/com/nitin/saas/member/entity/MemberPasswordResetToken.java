package com.nitin.saas.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Password reset token scoped to a Member (not a User).
 *
 * Separate from PasswordResetToken (which belongs to auth.entity) to avoid
 * cross-domain ID collisions and to allow independent cleanup scheduling.
 */
@Entity
@Table(name = "member_password_reset_tokens", indexes = {
        @Index(name = "idx_member_pwd_reset_token",     columnList = "token"),
        @Index(name = "idx_member_pwd_reset_member_id", columnList = "memberId"),
        @Index(name = "idx_member_pwd_reset_expires",   columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private Long memberId;

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

    @Column(length = 45)
    private String resetIpAddress;

    @Column(length = 255)
    private String resetUserAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    // ── Domain logic ──────────────────────────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed(String ipAddress, String userAgent) {
        this.used          = true;
        this.usedAt        = LocalDateTime.now();
        this.resetIpAddress  = ipAddress;
        this.resetUserAgent  = userAgent;
    }
}