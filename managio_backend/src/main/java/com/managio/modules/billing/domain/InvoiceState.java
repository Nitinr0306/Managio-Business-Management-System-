package com.managio.modules.billing.domain;

import com.managio.modules.billing.domain.Invoice.Status;
import java.math.BigDecimal;
import java.time.LocalDate;

/** BIL-2: an invoice's state, derived from its status, payments and due date. */
public enum InvoiceState {
    VOID, PAID, OVERDUE, PARTLY_PAID, DUE;

    /** Overdue wins over partly paid: money is late whether or not some of it has arrived. */
    public static InvoiceState of(Status status, BigDecimal total, BigDecimal paid, LocalDate dueOn, LocalDate today) {
        if (status == Status.VOID) {
            return VOID;
        }
        if (paid.compareTo(total) >= 0) {
            return PAID;
        }
        if (dueOn.isBefore(today)) {
            return OVERDUE;
        }
        return paid.signum() > 0 ? PARTLY_PAID : DUE;
    }
}
