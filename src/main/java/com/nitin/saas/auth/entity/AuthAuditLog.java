package com.nitin.saas.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_audit_logs", indexes = {
        @Index(name = "idx_auth_audit_user_id", columnList = "userId"),
        @Index(name = "idx_auth_audit_event", columnList = "eventType"),
        @Index(name = "idx_auth_audit_created", columnList = "createdAt"),
        @Index(name = "idx_auth_audit_status", columnList = "status"),
        @Index(name = "idx_auth_audit_ip", columnList = "ipAddress")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String deviceId;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 100)
    private String requestId;

    @Column(length = 100)
    private String sessionId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        REGISTER,
        EMAIL_VERIFICATION_SENT,
        EMAIL_VERIFIED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_SUCCESS,
        PASSWORD_CHANGED,
        TOKEN_REFRESHED,
        TOKEN_REVOKED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        TWO_FACTOR_ENABLED,
        TWO_FACTOR_DISABLED,
        TWO_FACTOR_VERIFIED,
        SUSPICIOUS_ACTIVITY,
        ROLE_CHANGED,
        ACCOUNT_DELETED
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        PENDING,
        BLOCKED
    }

    public static AuthAuditLog loginSuccess(Long userId, String email, String ipAddress, String userAgent) {
        return AuthAuditLog.builder()
                .userId(userId)
                .email(email)
                .eventType(EventType.LOGIN_SUCCESS)
                .status(Status.SUCCESS)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    public static AuthAuditLog loginFailed(String email, String ipAddress, String userAgent, String reason) {
        return AuthAuditLog.builder()
                .email(email)
                .eventType(EventType.LOGIN_FAILED)
                .status(Status.FAILURE)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .errorMessage(reason)
                .build();
    }

    public static AuthAuditLog suspiciousActivity(Long userId, String email, String ipAddress, String details) {
        return AuthAuditLog.builder()
                .userId(userId)
                .email(email)
                .eventType(EventType.SUSPICIOUS_ACTIVITY)
                .status(Status.BLOCKED)
                .ipAddress(ipAddress)
                .details(details)
                .build();
    }
}