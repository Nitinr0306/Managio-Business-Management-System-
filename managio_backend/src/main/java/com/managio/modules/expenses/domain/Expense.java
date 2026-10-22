package com.managio.modules.expenses.domain;

import com.managio.modules.billing.domain.PaymentMethod;
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

/** Money spent by the business. Voided with a reason, never deleted (EXP-2). */
@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private long categoryId;
    private BigDecimal amount;
    private LocalDate spentOn;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private String payee;
    private String description;
    private String reference;
    private Long payrollEntryId;
    private Long requestId;
    private Long recordedByUserId;
    private Instant voidedAt;
    private String voidReason;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static Expense record(long businessId, long categoryId, BigDecimal amount, LocalDate spentOn, PaymentMethod method,
                          String payee, String description, String reference, Long payrollEntryId, Long requestId,
                          long recordedByUserId) {
        Expense expense = new Expense();
        expense.businessId = businessId;
        expense.categoryId = categoryId;
        expense.amount = amount;
        expense.spentOn = spentOn;
        expense.method = method;
        expense.payee = payee;
        expense.description = description;
        expense.reference = reference;
        expense.payrollEntryId = payrollEntryId;
        expense.requestId = requestId;
        expense.recordedByUserId = recordedByUserId;
        return expense;
    }

    public boolean isVoid() {
        return voidedAt != null;
    }

    public void voidExpense(Instant now, String reason) {
        this.voidedAt = now;
        this.voidReason = reason;
    }
}
