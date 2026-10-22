package com.managio.modules.billing.service;

import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.domain.Payment;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.billing.repository.BillingQueries;
import com.managio.modules.billing.repository.InvoiceRepository;
import com.managio.modules.billing.repository.PaymentRepository;
import com.managio.modules.billing.dto.BillingDtos.PaymentResponse;
import com.managio.modules.billing.domain.DocumentPrefixes;
import com.managio.modules.business.service.BusinessSequences;
import com.managio.modules.business.service.BusinessSequences.Sequence;
import com.managio.modules.daybook.service.DayLock;
import com.managio.platform.util.Money;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/** Receipts, refunds and voids (BIL-3, BIL-5, BIL-6, BIL-7). Every money movement locks its invoice first. */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final BusinessSequences sequences;
    private final BillingViews views;
    private final BillingQueries queries;
    private final ActivityLog activity;
    private final DayLock dayLock;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(BusinessContext ctx, LocalDate from, LocalDate to, PaymentMethod method,
                                              Long customerId, int page, int size) {
        return queries.payments(ctx.businessId(), from, to, method, customerId, page, size);
    }

    /** What the person at the counter enters for a payment. {@code amount} null means "the whole balance". */
    public record PaymentInput(BigDecimal amount, PaymentMethod method, LocalDate paidOn, String reference, String notes) {
    }

    /** BIL-3: a payment is always against an invoice and never more than what is still owed. */
    @Transactional
    public PaymentResponse record(BusinessContext ctx, long invoiceId, PaymentInput input) {
        ctx.require(Permission.BILLING_COLLECT);
        Invoice invoice = lockInvoice(ctx.businessId(), invoiceId);
        if (invoice.isVoid()) {
            throw ApiException.rule("invoice.void", "This invoice was voided, so it can't take payments.");
        }
        BigDecimal balance = invoice.getTotal().subtract(payments.netPaid(invoiceId));
        BigDecimal amount = input.amount() == null ? balance : Money.of(input.amount());
        if (amount.signum() <= 0) {
            throw ApiException.invalidField("amount", balance.signum() <= 0
                    ? "This invoice is already paid" : "Enter an amount greater than zero");
        }
        if (amount.compareTo(balance) > 0) {
            throw ApiException.invalidField("amount", "Only " + Money.inr(balance) + " is left to pay on this invoice");
        }
        LocalDate paidOn = input.paidOn() != null ? input.paidOn() : ctx.today(clock);
        if (paidOn.isAfter(ctx.today(clock))) {
            throw ApiException.invalidField("paidOn", "The payment date can't be in the future");
        }
        dayLock.requireOpen(ctx.businessId(), paidOn, input.method() == PaymentMethod.CASH);
        Payment payment = payments.save(Payment.record(Payment.Kind.PAYMENT, ctx.businessId(),
                sequences.next(ctx.businessId(), Sequence.RECEIPT), invoice, null, amount, input.method(),
                Normalize.text(input.reference()), paidOn, clock.instant(), ctx.userId(), Normalize.text(input.notes())));
        DocumentPrefixes prefixes = views.prefixes(ctx.businessId());
        activity.record(ctx, "PAYMENT_RECORDED", "PAYMENT", payment.getId(), "Received " + Money.inr(amount) + " by "
                + methodName(input.method()) + " for " + prefixes.invoiceNumber(invoice.getNumber()) + " ("
                + prefixes.receiptNumber(payment.getNumber()) + ")");
        return views.payment(payment, prefixes, prefixes.invoiceNumber(invoice.getNumber()));
    }

    /** BIL-6: returns part or all of a payment; never more than what hasn't been returned yet. */
    @Transactional
    public PaymentResponse refund(BusinessContext ctx, long paymentId, BigDecimal amountInput, PaymentMethod method,
                                  String reason) {
        ctx.require(Permission.BILLING_VOID);
        Payment original = payments.findByIdAndBusinessId(paymentId, ctx.businessId())
                .orElseThrow(() -> ApiException.notFound("Payment"));
        Invoice invoice = lockInvoice(ctx.businessId(), original.getInvoiceId());
        if (original.getKind() != Payment.Kind.PAYMENT || original.isVoid()) {
            throw ApiException.rule("payment.not_refundable", "Only a payment that hasn't been voided can be refunded.");
        }
        BigDecimal refundable = original.getAmount().subtract(payments.refundedAmount(paymentId));
        BigDecimal amount = amountInput == null ? refundable : Money.of(amountInput);
        if (amount.signum() <= 0 || amount.compareTo(refundable) > 0) {
            throw ApiException.invalidField("amount", refundable.signum() <= 0
                    ? "This payment has already been fully refunded"
                    : "Enter an amount up to " + Money.inr(refundable));
        }
        String cleanReason = requireReason(reason);
        dayLock.requireOpen(ctx.businessId(), ctx.today(clock), method == PaymentMethod.CASH);
        Payment refund = payments.save(Payment.record(Payment.Kind.REFUND, ctx.businessId(),
                sequences.next(ctx.businessId(), Sequence.RECEIPT), invoice, paymentId, amount, method, null,
                ctx.today(clock), clock.instant(), ctx.userId(), cleanReason));
        DocumentPrefixes prefixes = views.prefixes(ctx.businessId());
        activity.record(ctx, "PAYMENT_REFUNDED", "PAYMENT", refund.getId(), "Refunded " + Money.inr(amount) + " of "
                + prefixes.receiptNumber(original.getNumber()) + ": " + cleanReason);
        return views.payment(refund, prefixes, prefixes.invoiceNumber(invoice.getNumber()));
    }

    /** BIL-5: a payment recorded by mistake is voided with a reason, never deleted. */
    @Transactional
    public PaymentResponse voidPayment(BusinessContext ctx, long paymentId, String reason) {
        ctx.require(Permission.BILLING_VOID);
        Payment payment = payments.findByIdAndBusinessId(paymentId, ctx.businessId())
                .orElseThrow(() -> ApiException.notFound("Payment"));
        Invoice invoice = lockInvoice(ctx.businessId(), payment.getInvoiceId());
        if (payment.isVoid()) {
            throw ApiException.rule("payment.already_void", "This payment was already voided.");
        }
        if (payment.getKind() == Payment.Kind.PAYMENT && payments.refundedAmount(paymentId).signum() > 0) {
            throw ApiException.rule("payment.has_refunds", "Void the refunds of this payment first.");
        }
        String cleanReason = requireReason(reason);
        dayLock.requireOpen(ctx.businessId(), payment.getPaidOn(), payment.getMethod() == PaymentMethod.CASH);
        payment.voidPayment(clock.instant(), cleanReason);
        DocumentPrefixes prefixes = views.prefixes(ctx.businessId());
        activity.record(ctx, "PAYMENT_VOIDED", "PAYMENT", paymentId, "Voided " + prefixes.receiptNumber(payment.getNumber())
                + " of " + Money.inr(payment.getAmount()) + ": " + cleanReason);
        return views.payment(payment, prefixes, prefixes.invoiceNumber(invoice.getNumber()));
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(BusinessContext ctx, long paymentId) {
        Payment payment = payments.findByIdAndBusinessId(paymentId, ctx.businessId())
                .orElseThrow(() -> ApiException.notFound("Payment"));
        Invoice invoice = invoices.findByIdAndBusinessId(payment.getInvoiceId(), ctx.businessId()).orElseThrow();
        DocumentPrefixes prefixes = views.prefixes(ctx.businessId());
        return views.payment(payment, prefixes, prefixes.invoiceNumber(invoice.getNumber()));
    }

    private Invoice lockInvoice(long businessId, long invoiceId) {
        return invoices.findForUpdate(invoiceId, businessId).orElseThrow(() -> ApiException.notFound("Invoice"));
    }

    private static String requireReason(String reason) {
        String clean = Normalize.text(reason);
        if (clean == null) {
            throw ApiException.invalidField("reason", "Say why, so the activity log makes sense later");
        }
        if (clean.length() > 300) {
            throw ApiException.invalidField("reason", "Use 300 characters or fewer");
        }
        return clean;
    }

    private static String methodName(PaymentMethod method) {
        return switch (method) {
            case CASH -> "cash";
            case UPI -> "UPI";
            case CARD -> "card";
            case BANK_TRANSFER -> "bank transfer";
            case CHEQUE -> "cheque";
            case ONLINE -> "online payment";
            case OTHER -> "other method";
        };
    }
}
