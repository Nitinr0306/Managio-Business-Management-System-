package com.managio.modules.activity.domain;

import jakarta.persistence.Column;
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

@Entity
@Table(name = "activity_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityEvent {

    public enum ActorType { USER, CUSTOMER, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;

    @Enumerated(EnumType.STRING)
    private ActorType actorType;

    private Long actorId;
    private String actorName;
    private String action;
    private String entityType;
    private Long entityId;
    private String summary;
    private String ipAddress;

    @Column(updatable = false)
    private Instant createdAt;

    public ActivityEvent(long businessId, ActorType actorType, Long actorId, String actorName, String action,
                  String entityType, Long entityId, String summary, String ipAddress, Instant createdAt) {
        this.businessId = businessId;
        this.actorType = actorType;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary.length() > 500 ? summary.substring(0, 497) + "..." : summary;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }
}
