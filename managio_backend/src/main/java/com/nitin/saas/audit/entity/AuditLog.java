package com.nitin.saas.audit.entity;

import com.nitin.saas.common.utils.PublicIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_business", columnList = "businessId"),
        @Index(name = "idx_audit_user", columnList = "userId"),
        @Index(name = "idx_audit_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String logId;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 20)
    private String actorType;

    @Column(length = 20)
    private String actorPublicId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 50)
    private String entityType;

    private Long entityId;

    @Column(length = 30)
    private String entityPublicId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void assignLogIdIfMissing() {
        if (logId == null || logId.isBlank()) {
            logId = PublicIdGenerator.generate("L", 11);
        }
    }
}