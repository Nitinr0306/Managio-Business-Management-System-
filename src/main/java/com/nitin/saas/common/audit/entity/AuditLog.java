package com.nitin.saas.common.audit.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@org.hibernate.annotations.Immutable
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column
    private Long entityId;


    @Column
    private Long actorUserId;

    @Column(length = 255)
    private String reason;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    protected AuditLog() {}

    public AuditLog(
            String action,
            String entityType,
            Long entityId,
            Long actorUserId,
            String reason
    ) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorUserId = actorUserId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
