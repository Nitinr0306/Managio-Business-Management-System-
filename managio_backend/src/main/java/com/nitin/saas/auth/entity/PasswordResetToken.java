package com.nitin.saas.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_pwd_reset_token",   columnList = "token"),
        @Index(name = "idx_pwd_reset_user_id", columnList = "userId"),
        @Index(name = "idx_pwd_reset_expires", columnList = "expiresAt")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 500) private String token;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) @Builder.Default private Boolean used = false;
    private LocalDateTime usedAt;
    @Column(length = 45)  private String requestIpAddress;
    @Column(length = 255) private String requestUserAgent;
    @Column(length = 45)  private String resetIpAddress;
    @Column(length = 255) private String resetUserAgent;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Version private Long version;

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isValid()   { return !used && !isExpired(); }
    public void markAsUsed(String ip, String ua) {
        used = true; usedAt = LocalDateTime.now();
        resetIpAddress = ip; resetUserAgent = ua;
    }
}