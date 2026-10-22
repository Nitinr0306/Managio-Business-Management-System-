package com.managio.modules.billing.domain;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Money received (PAYMENT) or returned (REFUND), always against an invoice. Voided, never deleted. */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    public enum Kind { PAYMENT, REFUND }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private int number;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    private Long customerId;
    private long invoiceId;
    private Long refundOfId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private String reference;
    private LocalDate paidOn;
    private Instant receivedAt;
    private Long receivedByUserId;
    private String notes;
    private Instant voidedAt;
    private String voidReason;

    @CreationTimestamp
    private Instant createdAt;
    @Version
    private long version;

    public static Payment record(Kind kind, long businessId, int number, Invoice invoice, Long refundOfId, BigDecimal amount,
                          PaymentMethod method, String reference, LocalDate paidOn, Instant receivedAt,
                          long receivedByUserId, String notes) {
        Payment payment = new Payment();
        payment.kind = kind;
        payment.businessId = businessId;
        payment.number = number;
        payment.customerId = invoice.getCustomerId();
        payment.invoiceId = invoice.getId();
        payment.refundOfId = refundOfId;
        payment.amount = amount;
        payment.method = method;
        payment.reference = reference;
        payment.paidOn = paidOn;
        payment.receivedAt = receivedAt;
        payment.receivedByUserId = receivedByUserId;
        payment.notes = notes;
        return payment;
    }

    public boolean isVoid() {
        return voidedAt != null;
    }

    public void voidPayment(Instant now, String reason) {
        this.voidedAt = now;
        this.voidReason = reason;
    }
}
