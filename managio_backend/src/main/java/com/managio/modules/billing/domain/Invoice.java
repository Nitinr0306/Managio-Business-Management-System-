package com.managio.modules.billing.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
import java.util.ArrayList;
import java.util.List;

/** What a customer owes (or a walk-in sale). Whether it is paid, due or overdue is derived from payments. */
@Entity
@Table(name = "invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    public enum Status { ISSUED, VOID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private int number;
    private Long customerId;
    private Long membershipId;
    private LocalDate issuedOn;
    private LocalDate dueOn;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private String notes;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ISSUED;

    private Instant voidedAt;
    private String voidReason;
    private Long createdByUserId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    @OrderBy("position")
    private List<InvoiceLine> lines = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static Invoice issue(long businessId, int number, Long customerId, Long membershipId, LocalDate issuedOn,
                         LocalDate dueOn, InvoiceMath.Totals totals, List<InvoiceLine> lines, String notes,
                         long createdByUserId) {
        Invoice invoice = new Invoice();
        invoice.businessId = businessId;
        invoice.number = number;
        invoice.customerId = customerId;
        invoice.membershipId = membershipId;
        invoice.issuedOn = issuedOn;
        invoice.dueOn = dueOn;
        invoice.subtotal = totals.subtotal();
        invoice.discount = totals.discount();
        invoice.tax = totals.tax();
        invoice.total = totals.total();
        invoice.notes = notes;
        invoice.createdByUserId = createdByUserId;
        invoice.lines.addAll(lines);
        return invoice;
    }

    public boolean isVoid() {
        return status == Status.VOID;
    }

    /** Replaces the money figures after a discount is added; the lines don't change. */
    public void reprice(InvoiceMath.Totals totals) {
        this.discount = totals.discount();
        this.tax = totals.tax();
        this.total = totals.total();
    }

    public void voidInvoice(Instant now, String reason) {
        this.status = Status.VOID;
        this.voidedAt = now;
        this.voidReason = reason;
    }
}
