package com.managio.modules.requests.domain;

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

/** Something a team member asks the business to approve: an expense, a purchase, leave, a discount, or anything else. */
@Entity
@Table(name = "requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalRequest {

    public enum Type { EXPENSE, PURCHASE, LEAVE, DISCOUNT, OTHER }

    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String title;
    private String details;
    private BigDecimal amount;
    private Long expenseCategoryId;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private long requestedByMemberId;
    private Long decidedByUserId;
    private Instant decidedAt;
    private String decisionNote;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    /** What the requester fills in. Which fields are needed depends on the type. */
    public record Details(Type type, String title, String details, BigDecimal amount, Long expenseCategoryId,
                          LocalDate startsOn, LocalDate endsOn, Long invoiceId) {
    }

    public static ApprovalRequest create(long businessId, long requestedByMemberId, Details d) {
        ApprovalRequest request = new ApprovalRequest();
        request.businessId = businessId;
        request.requestedByMemberId = requestedByMemberId;
        request.type = d.type();
        request.title = d.title();
        request.details = d.details();
        request.amount = d.amount();
        request.expenseCategoryId = d.expenseCategoryId();
        request.startsOn = d.startsOn();
        request.endsOn = d.endsOn();
        request.invoiceId = d.invoiceId();
        return request;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public void decide(Status status, long userId, Instant now, String note) {
        this.status = status;
        this.decidedByUserId = userId;
        this.decidedAt = now;
        this.decisionNote = note;
    }

    public void cancel(Instant now) {
        this.status = Status.CANCELLED;
        this.decidedAt = now;
    }
}
