package com.nitin.saas.auth.entity;

import jakarta.persistence.*;
import com.nitin.saas.common.utils.PublicIdGenerator;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_audit_logs", indexes = {
        @Index(name = "idx_auth_audit_user_id", columnList = "userId"),
        @Index(name = "idx_auth_audit_event",   columnList = "eventType"),
        @Index(name = "idx_auth_audit_created", columnList = "createdAt"),
        @Index(name = "idx_auth_audit_status",  columnList = "status"),
        @Index(name = "idx_auth_audit_ip",      columnList = "ipAddress")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthAuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 20) private String eventId;
    private Long userId;
    @Column(length = 255) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private EventType eventType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(length = 45)  private String ipAddress;
    @Column(length = 500) private String userAgent;
    @Column(length = 100) private String deviceId;
    @Column(length = 100) private String location;
    @Column(columnDefinition = "TEXT") private String details;
    @Column(columnDefinition = "TEXT") private String errorMessage;
    @Column(length = 100) private String requestId;
    @Column(length = 100) private String sessionId;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    private void assignEventIdIfMissing() {
        if (eventId == null || eventId.isBlank()) {
            eventId = PublicIdGenerator.generate("A", 11);
        }
    }

    public enum EventType {
        LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, REGISTER,
        EMAIL_VERIFICATION_SENT, EMAIL_VERIFIED,
        PASSWORD_RESET_REQUESTED, PASSWORD_RESET_SUCCESS, PASSWORD_CHANGED,
        TOKEN_REFRESHED, TOKEN_REVOKED,
        ACCOUNT_LOCKED, ACCOUNT_UNLOCKED,
        TWO_FACTOR_ENABLED, TWO_FACTOR_DISABLED, TWO_FACTOR_VERIFIED,
        SUSPICIOUS_ACTIVITY, ROLE_CHANGED, ACCOUNT_DELETED
    }

    public enum Status { SUCCESS, FAILURE, PENDING, BLOCKED }

    public static AuthAuditLog loginSuccess(Long userId, String email, String ip, String ua) {
        return AuthAuditLog.builder().userId(userId).email(email)
                .eventType(EventType.LOGIN_SUCCESS).status(Status.SUCCESS)
                .ipAddress(ip).userAgent(ua).build();
    }
    public static AuthAuditLog loginFailed(String email, String ip, String ua, String reason) {
        return AuthAuditLog.builder().email(email)
                .eventType(EventType.LOGIN_FAILED).status(Status.FAILURE)
                .ipAddress(ip).userAgent(ua).errorMessage(reason).build();
    }
}