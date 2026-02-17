package com.nitin.saas.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token", columnList = "token"),
        @Index(name = "idx_refresh_user_id", columnList = "userId"),
        @Index(name = "idx_refresh_expires", columnList = "expiresAt"),
        @Index(name = "idx_refresh_revoked", columnList = "revoked")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    private LocalDateTime revokedAt;

    private LocalDateTime usedAt;

    @Column(length = 500)
    private String replacedByToken;

    @Column(length = 100)
    private String deviceId;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer rotationCount = 0;

    private LocalDateTime lastRotatedAt;

    @Version
    private Long version;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !used && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    public void rotate(String newToken) {
        this.replacedByToken = newToken;
        this.rotationCount++;
        this.lastRotatedAt = LocalDateTime.now();
        this.revoke();
    }
}