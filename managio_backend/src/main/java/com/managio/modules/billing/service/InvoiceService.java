package com.managio.modules.billing.service;

import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.domain.InvoiceLine;
import com.managio.modules.billing.domain.InvoiceMath;
import com.managio.modules.billing.repository.BillingQueries;
import com.managio.modules.billing.repository.BillingQueries.InvoiceView;
import com.managio.modules.billing.repository.InvoiceRepository;
import com.managio.modules.billing.repository.PaymentRepository;
import com.managio.modules.billing.dto.BillingDtos.InvoiceResponse;
import com.managio.modules.billing.dto.BillingDtos.InvoiceRow;
import com.managio.modules.billing.dto.BillingDtos.SaleResponse;
import com.managio.modules.billing.domain.InvoiceMath.LineInput;
import com.managio.modules.billing.domain.InvoiceMath.Totals;
import com.managio.modules.billing.service.PaymentService.PaymentInput;
import com.managio.modules.business.service.BusinessSequences;
import com.managio.modules.business.service.BusinessSequences.Sequence;
import com.managio.modules.customers.service.CustomerService;
import com.managio.modules.catalog.domain.Offering;
import com.managio.modules.catalog.service.OfferingService;
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
import java.util.ArrayList;
import java.util.List;

/** Creating and voiding invoices (BIL-1, BIL-4, BIL-5, BIL-8, BIL-9, MEM-4). */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final BusinessSequences sequences;
    private final CustomerService customers;
    private final OfferingService offerings;
    private final PaymentService paymentService;
    private final BillingViews views;
    private final BillingQueries queries;
    private final ActivityLog activity;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<InvoiceRow> list(BusinessContext ctx, InvoiceView view, Long customerId, String search,
                                         int page, int size) {
        return queries.invoices(ctx.businessId(), ctx.today(clock), view, customerId, search, page, size);
    }

    /**
     * One line of an invoice: a catalogue item ({@code offeringId}) or a custom line ({@code description} and
     * {@code unitPrice}). {@code unitPrice} on a catalogue item overrides its price and needs billing.discount.
     */
    public record LineRequest(Long offeringId, String description, BigDecimal quantity, BigDecimal unitPrice,
                              BigDecimal taxRate) {
    }

    public record InvoiceRequest(Long customerId, List<LineRequest> lines, BigDecimal discount, LocalDate dueOn,
                                 String notes) {
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(BusinessContext ctx, long invoiceId) {
        return views.invoice(load(ctx.businessId(), invoiceId), ctx.today(clock));
    }

    /** The invoices raised for a membership, oldest first. */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> forMembership(BusinessContext ctx, long membershipId) {
        return invoices.findByBusinessIdAndMembershipIdOrderByIdAsc(ctx.businessId(), membershipId).stream()
                .map(i -> views.invoice(i, ctx.today(clock))).toList();
    }

    @Transactional
    public InvoiceResponse create(BusinessContext ctx, InvoiceRequest request) {
        return views.invoice(createInvoice(ctx, request), ctx.today(clock));
    }

    /** BIL-3: invoice and payment in one action; the payment is the full total unless {@code payment.amount} says less. */
    @Transactional
    public SaleResponse sell(BusinessContext ctx, InvoiceRequest request, PaymentInput payment) {
        ctx.require(Permission.BILLING_COLLECT);
        Invoice invoice = createInvoice(ctx, new InvoiceRequest(request.customerId(), request.lines(), request.discount(),
                ctx.today(clock), request.notes()), true, payment.amount());
        var paymentResponse = invoice.getTotal().signum() == 0 ? null
                : paymentService.record(ctx, invoice.getId(), payment);
        return new SaleResponse(views.invoice(invoice, ctx.today(clock)), paymentResponse);
    }

    /**
     * MEM-4: invoices for a membership. A plan with N installments creates N invoices that split the plan
     * price, due at evenly spaced dates from the start. MEM-9: the discount applies to the first invoice.
     */
    @Transactional
    public List<Invoice> createForMembership(BusinessContext ctx, long membershipId, long customerId, Offering plan,
                                             BigDecimal price, LocalDate startsOn, LocalDate endsOn, BigDecimal discount) {
        int parts = plan.getInstallments();
        List<BigDecimal> shares = InvoiceMath.splitEvenly(price, parts);
        List<LocalDate> dueDates = InvoiceMath.installmentDueDates(startsOn, endsOn, parts);
        BigDecimal remainingDiscount = Money.of(discount);
        if (remainingDiscount.signum() > 0) {
            ctx.require(Permission.BILLING_DISCOUNT);
        }
        List<Invoice> created = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            String description = parts == 1 ? plan.getName() : "%s (%d of %d)".formatted(plan.getName(), i + 1, parts);
            BigDecimal invoiceDiscount = i == 0 ? remainingDiscount : Money.ZERO;
            Totals totals = InvoiceMath.compute(
                    List.of(new LineInput(BigDecimal.ONE, shares.get(i), plan.getTaxRate())), invoiceDiscount);
            InvoiceLine line = new InvoiceLine(plan.getId(), description, BigDecimal.ONE, shares.get(i), plan.getTaxRate(),
                    totals.lineAmounts().getFirst(), 0);
            Invoice invoice = Invoice.issue(ctx.businessId(), sequences.next(ctx.businessId(), Sequence.INVOICE), customerId,
                    membershipId, startsOn, dueDates.get(i), totals, List.of(line), null, ctx.userId());
            created.add(invoices.save(invoice));
        }
        return created;
    }

    /** MEM-6: cancelling a membership voids its invoices that are unpaid and not yet due. */
    @Transactional
    public int voidUnpaidFuture(BusinessContext ctx, long membershipId, String reason) {
        int voided = 0;
        LocalDate today = ctx.today(clock);
        for (Invoice invoice : invoices.findByBusinessIdAndMembershipIdOrderByIdAsc(ctx.businessId(), membershipId)) {
            if (!invoice.isVoid() && invoice.getDueOn().isAfter(today) && payments.netPaid(invoice.getId()).signum() == 0
                    && payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).isEmpty()) {
                invoice.voidInvoice(clock.instant(), reason);
                voided++;
            }
        }
        return voided;
    }

    /** BIL-5: needs billing.void and a reason, and no money may be left on the invoice. BIL-8: stock goes back. */
    @Transactional
    public InvoiceResponse voidInvoice(BusinessContext ctx, long invoiceId, String reason) {
        ctx.require(Permission.BILLING_VOID);
        Invoice invoice = invoices.findForUpdate(invoiceId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Invoice"));
        if (invoice.isVoid()) {
            throw ApiException.rule("invoice.already_void", "This invoice was already voided.");
        }
        if (payments.netPaid(invoiceId).signum() != 0) {
            throw ApiException.rule("invoice.has_payments",
                    "This invoice has payments. Void or refund them first, then void the invoice.");
        }
        String clean = Normalize.text(reason);
        if (clean == null) {
            throw ApiException.invalidField("reason", "Say why, so the activity log makes sense later");
        }
        invoice.voidInvoice(clock.instant(), clean.length() > 300 ? clean.substring(0, 300) : clean);
        for (InvoiceLine line : invoice.getLines()) {
            if (line.getOfferingId() != null && line.getQuantity().stripTrailingZeros().scale() <= 0) {
                offerings.returnStock(ctx.businessId(), line.getOfferingId(), line.getQuantity().intValueExact());
            }
        }
        var prefixes = views.prefixes(ctx.businessId());
        activity.record(ctx, "INVOICE_VOIDED", "INVOICE", invoiceId,
                "Voided " + prefixes.invoiceNumber(invoice.getNumber()) + ": " + clean);
        return views.invoice(invoice, ctx.today(clock));
    }

    /**
     * REQ-3: adds an approved discount to an invoice that has no payments yet. The approver's authority comes
     * from approving the request, so billing.discount isn't checked here.
     */
    @Transactional
    public void addApprovedDiscount(BusinessContext ctx, long invoiceId, BigDecimal amount) {
        Invoice invoice = invoices.findForUpdate(invoiceId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Invoice"));
        if (invoice.isVoid() || !payments.findByInvoiceIdOrderByIdAsc(invoiceId).isEmpty()) {
            throw ApiException.rule("invoice.not_discountable",
                    "The discount can't be applied: the invoice is void or already has payments.");
        }
        List<LineInput> lines = invoice.getLines().stream()
                .map(l -> new LineInput(l.getQuantity(), l.getUnitPrice(), l.getTaxRate())).toList();
        invoice.reprice(InvoiceMath.compute(lines, invoice.getDiscount().add(Money.of(amount))));
        var prefixes = views.prefixes(ctx.businessId());
        activity.record(ctx, "INVOICE_DISCOUNTED", "INVOICE", invoiceId, "Discounted " + prefixes.invoiceNumber(invoice.getNumber())
                + " by " + Money.inr(amount));
    }

    @Transactional(readOnly = true)
    public Invoice load(long businessId, long invoiceId) {
        return invoices.findByIdAndBusinessId(invoiceId, businessId).orElseThrow(() -> ApiException.notFound("Invoice"));
    }

    private Invoice createInvoice(BusinessContext ctx, InvoiceRequest request) {
        return createInvoice(ctx, request, false, null);
    }

    /**
     * A walk-in invoice without a customer is only allowed for a sale paid in full: {@code sale} is true and
     * {@code payNow} is null (the whole total) or at least the total. Anything owed needs a customer to owe it.
     */
    private Invoice createInvoice(BusinessContext ctx, InvoiceRequest request, boolean sale, BigDecimal payNow) {
        ctx.require(Permission.BILLING_COLLECT);
        if (request.lines() == null || request.lines().isEmpty()) {
            throw ApiException.invalidField("lines", "Add at least one item");
        }
        if (request.customerId() != null) {
            customers.requireActive(ctx.businessId(), request.customerId());
        }
        BigDecimal discount = Money.of(request.discount());
        if (discount.signum() > 0) {
            ctx.require(Permission.BILLING_DISCOUNT);
        }
        List<InvoiceLine> lines = new ArrayList<>();
        List<LineInput> inputs = new ArrayList<>();
        List<ResolvedLine> resolved = new ArrayList<>();
        for (LineRequest line : request.lines()) {
            resolved.add(resolve(ctx, line));
        }
        for (ResolvedLine r : resolved) {
            inputs.add(new LineInput(r.quantity(), r.unitPrice(), r.taxRate()));
        }
        Totals totals = InvoiceMath.compute(inputs, discount);
        if (request.customerId() == null) {
            if (!sale) {
                throw ApiException.invalidField("customerId", "Choose the customer this invoice is for");
            }
            if (payNow != null && payNow.compareTo(totals.total()) < 0) {
                throw ApiException.invalidField("customerId", "Choose a customer to record the amount still owed");
            }
        }
        for (int i = 0; i < resolved.size(); i++) {
            ResolvedLine r = resolved.get(i);
            lines.add(new InvoiceLine(r.offeringId(), r.description(), r.quantity(), r.unitPrice(), r.taxRate(),
                    totals.lineAmounts().get(i), i));
        }
        LocalDate issuedOn = ctx.today(clock);
        LocalDate dueOn = request.dueOn() != null ? request.dueOn() : issuedOn;
        if (dueOn.isBefore(issuedOn)) {
            throw ApiException.invalidField("dueOn", "The due date can't be before today");
        }
        for (ResolvedLine r : resolved) {
            if (r.stockProductId() != null) {
                offerings.takeStock(ctx.businessId(), r.stockProductId(), r.quantity().intValueExact());
            }
        }
        Invoice invoice = invoices.save(Invoice.issue(ctx.businessId(), sequences.next(ctx.businessId(), Sequence.INVOICE),
                request.customerId(), null, issuedOn, dueOn, totals, lines, Normalize.text(request.notes()), ctx.userId()));
        var prefixes = views.prefixes(ctx.businessId());
        activity.record(ctx, "INVOICE_CREATED", "INVOICE", invoice.getId(),
                "Created " + prefixes.invoiceNumber(invoice.getNumber()) + " for " + Money.inr(totals.total()));
        return invoice;
    }

    private record ResolvedLine(Long offeringId, String description, BigDecimal quantity, BigDecimal unitPrice,
                                BigDecimal taxRate, Long stockProductId) {
    }

    /** BIL-9: catalogue lines come from active non-plan offerings; price overrides need billing.discount. */
    private ResolvedLine resolve(BusinessContext ctx, LineRequest line) {
        BigDecimal quantity = line.quantity() == null ? BigDecimal.ONE : line.quantity();
        if (quantity.signum() <= 0) {
            throw ApiException.invalidField("lines", "Quantities must be greater than zero");
        }
        if (line.offeringId() == null) {
            String description = Normalize.text(line.description());
            if (description == null || line.unitPrice() == null || line.unitPrice().signum() < 0) {
                throw ApiException.invalidField("lines", "Custom items need a description and a price");
            }
            BigDecimal tax = line.taxRate() == null ? Money.ZERO : line.taxRate();
            if (tax.signum() < 0 || tax.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw ApiException.invalidField("lines", "Tax rate must be between 0 and 100");
            }
            return new ResolvedLine(null, description, quantity, Money.of(line.unitPrice()), tax, null);
        }
        Offering offering = offerings.get(ctx.businessId(), line.offeringId());
        if (!offering.isActive()) {
            throw ApiException.rule("offering.inactive", offering.getName() + " is no longer sold.");
        }
        if (offering.getKind() == Offering.Kind.PLAN) {
            throw ApiException.rule("invoice.plan_line",
                    offering.getName() + " is a plan. Start a membership for the customer instead, so it has dates.");
        }
        BigDecimal price = offering.getPrice();
        if (line.unitPrice() != null && Money.of(line.unitPrice()).compareTo(price) != 0) {
            ctx.require(Permission.BILLING_DISCOUNT);
            price = Money.of(line.unitPrice());
        }
        Long stockId = null;
        if (offering.isTrackStock()) {
            if (quantity.stripTrailingZeros().scale() > 0) {
                throw ApiException.invalidField("lines", offering.getName() + " is sold in whole units");
            }
            stockId = offering.getId();
        }
        return new ResolvedLine(offering.getId(), offering.getName(), quantity, price, offering.getTaxRate(), stockId);
    }
}
