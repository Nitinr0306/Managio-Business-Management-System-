package com.nitin.saas.subscription.entity;

import com.nitin.saas.subscription.enums.SubscriptionStatus;
import com.nitin.saas.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "member_subscriptions")
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Member member;

    @ManyToOne(optional = false)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;


    @Column
    private String actionReason;

    protected MemberSubscription() {}

    public MemberSubscription(
            Member member,
            SubscriptionPlan plan,
            LocalDate startDate
    ) {
        this.member = member;
        this.plan = plan;
        this.startDate = startDate;
        this.endDate = startDate.plusDays(plan.getDurationInDays());
        this.status = SubscriptionStatus.PENDING;
    }

    public void markExpired() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void expire(LocalDate endDate, String reason) {
        this.status = SubscriptionStatus.EXPIRED;
        this.endDate = endDate;
        this.actionReason = reason;
    }

    public void cancel(LocalDate endDate, String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.endDate = endDate;
        this.actionReason = reason;
    }
    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
    }

}

