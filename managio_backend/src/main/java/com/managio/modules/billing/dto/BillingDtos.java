package com.managio.modules.billing.dto;

import com.managio.modules.billing.domain.InvoiceState;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.platform.web.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Response shapes for invoices and payments. */
public final class BillingDtos {

    private BillingDtos() {
    }

    public record CustomerRef(long id, int number, String name) {
    }

    public record LineResponse(@Nullable Long offeringId, String description, BigDecimal quantity, BigDecimal unitPrice,
                               BigDecimal taxRate, BigDecimal amount) {
    }

    public record PaymentResponse(long id, int number, String receiptNumber, String kind, long invoiceId,
                                  String invoiceNumber, @Nullable CustomerRef customer, BigDecimal amount,
                                  PaymentMethod method, @Nullable String reference, LocalDate paidOn,
                                  Instant receivedAt, @Nullable String receivedBy, @Nullable String notes,
                                  @Nullable Long refundOfId, boolean voided, @Nullable String voidReason) {
    }

    public record InvoiceRow(long id, int number, String invoiceNumber, @Nullable CustomerRef customer,
                             @Nullable Long membershipId, LocalDate issuedOn, LocalDate dueOn, BigDecimal total,
                             BigDecimal paid, BigDecimal balance, InvoiceState state) {
    }

    public record InvoiceResponse(long id, int number, String invoiceNumber, @Nullable CustomerRef customer,
                                  @Nullable Long membershipId, LocalDate issuedOn, LocalDate dueOn, BigDecimal subtotal,
                                  BigDecimal discount, BigDecimal tax, BigDecimal total, BigDecimal paid,
                                  BigDecimal balance, InvoiceState state, @Nullable String notes,
                                  @Nullable String voidReason, List<LineResponse> lines, List<PaymentResponse> payments) {
    }

    /** The outcome of a sale: the invoice with its payment. */
    public record SaleResponse(InvoiceResponse invoice, @Nullable PaymentResponse payment) {
    }
}
