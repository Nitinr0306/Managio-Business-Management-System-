package com.managio.modules.memberships.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A customer's plan from {@code startsOn} to {@code endsOn}, both inclusive (MEM-1). */
@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership {

    public enum Status { ACTIVE, CANCELLED }

    /** MEM-2: what a membership is like today. */
    public enum State { UPCOMING, ACTIVE, ENDING_SOON, EXPIRED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private long customerId;
    private long offeringId;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private Instant cancelledAt;
    private String cancelReason;
    private Long renewedFromId;
    private Long createdByUserId;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static Membership start(long businessId, long customerId, long offeringId, LocalDate startsOn, LocalDate endsOn,
                            BigDecimal price, Long renewedFromId, long createdByUserId) {
        Membership membership = new Membership();
        membership.businessId = businessId;
        membership.customerId = customerId;
        membership.offeringId = offeringId;
        membership.startsOn = startsOn;
        membership.endsOn = endsOn;
        membership.price = price;
        membership.renewedFromId = renewedFromId;
        membership.createdByUserId = createdByUserId;
        return membership;
    }

    public State state(LocalDate today) {
        if (status == Status.CANCELLED) {
            return State.CANCELLED;
        }
        if (startsOn.isAfter(today)) {
            return State.UPCOMING;
        }
        if (endsOn.isBefore(today)) {
            return State.EXPIRED;
        }
        return ChronoUnit.DAYS.between(today, endsOn) < MembershipState.ENDING_SOON_DAYS ? State.ENDING_SOON : State.ACTIVE;
    }

    public void cancel(Instant now, String reason) {
        this.status = Status.CANCELLED;
        this.cancelledAt = now;
        this.cancelReason = reason;
    }
}
