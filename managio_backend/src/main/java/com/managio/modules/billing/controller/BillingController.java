package com.managio.modules.billing.controller;

import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.billing.service.InvoiceService;
import com.managio.modules.billing.service.PaymentService;
import com.managio.modules.billing.dto.BillingDtos.InvoiceResponse;
import com.managio.modules.billing.dto.BillingDtos.InvoiceRow;
import com.managio.modules.billing.dto.BillingDtos.PaymentResponse;
import com.managio.modules.billing.dto.BillingDtos.SaleResponse;
import com.managio.modules.billing.repository.BillingQueries.InvoiceView;
import com.managio.modules.billing.service.InvoiceService.InvoiceRequest;
import com.managio.modules.billing.service.InvoiceService.LineRequest;
import com.managio.modules.billing.service.PaymentService.PaymentInput;
import com.managio.platform.persistence.IdempotentRequests;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.platform.web.DateRanges;
import com.managio.platform.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
@RequiredArgsConstructor
class BillingController {

    private final InvoiceService invoices;
    private final PaymentService payments;
    private final IdempotentRequests idempotency;

    record LineBody(Long offeringId, @Size(max = 200) String description, BigDecimal quantity, BigDecimal unitPrice,
                    BigDecimal taxRate) {
        LineRequest toRequest() {
            return new LineRequest(offeringId, description, quantity, unitPrice, taxRate);
        }
    }

    record InvoiceBody(Long customerId, @NotEmpty(message = "Add at least one item") List<@Valid LineBody> lines,
                       BigDecimal discount, LocalDate dueOn, @Size(max = 500) String notes) {
        InvoiceRequest toRequest() {
            return new InvoiceRequest(customerId, lines.stream().map(LineBody::toRequest).toList(), discount, dueOn, notes);
        }
    }

    record PaymentBody(BigDecimal amount, @NotNull(message = "Choose how it was paid") PaymentMethod method, LocalDate paidOn,
                       @Size(max = 100) String reference, @Size(max = 500) String notes) {
        PaymentInput toInput() {
            return new PaymentInput(amount, method, paidOn, reference, notes);
        }
    }

    record SaleBody(@NotNull @Valid InvoiceBody invoice, @NotNull @Valid PaymentBody payment) {
    }

    record ReasonBody(@NotNull(message = "Say why") @Size(max = 300) String reason) {
    }

    record RefundBody(BigDecimal amount, @NotNull(message = "Choose how it was returned") PaymentMethod method,
                      @NotNull(message = "Say why") @Size(max = 300) String reason) {
    }

    @GetMapping("/invoices")
    @RequirePermission(Permission.BILLING_VIEW)
    PageResponse<InvoiceRow> listInvoices(BusinessContext ctx, @RequestParam(defaultValue = "ALL") InvoiceView view,
                                          @RequestParam(required = false) Long customerId,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return invoices.list(ctx, view, customerId, q, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/invoices/{invoiceId}")
    @RequirePermission(Permission.BILLING_VIEW)
    InvoiceResponse getInvoice(BusinessContext ctx, @PathVariable long invoiceId) {
        return invoices.get(ctx, invoiceId);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.BILLING_COLLECT)
    InvoiceResponse createInvoice(BusinessContext ctx, @Valid @RequestBody InvoiceBody body, @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "invoice", body, InvoiceResponse.class,
                () -> invoices.create(ctx, body.toRequest()));
    }

    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.BILLING_COLLECT)
    SaleResponse sell(BusinessContext ctx, @Valid @RequestBody SaleBody body, @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "sale", body, SaleResponse.class,
                () -> invoices.sell(ctx, body.invoice().toRequest(), body.payment().toInput()));
    }

    @PostMapping("/invoices/{invoiceId}/void")
    @RequirePermission(Permission.BILLING_VOID)
    InvoiceResponse voidInvoice(BusinessContext ctx, @PathVariable long invoiceId, @Valid @RequestBody ReasonBody body) {
        return invoices.voidInvoice(ctx, invoiceId, body.reason());
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.BILLING_COLLECT)
    PaymentResponse recordPayment(BusinessContext ctx, @PathVariable long invoiceId, @Valid @RequestBody PaymentBody body,
                                  @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "payment:invoice:" + invoiceId, body,
                PaymentResponse.class, () -> payments.record(ctx, invoiceId, body.toInput()));
    }

    @GetMapping("/payments")
    @RequirePermission(Permission.BILLING_VIEW)
    PageResponse<PaymentResponse> listPayments(BusinessContext ctx,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               @RequestParam(required = false) PaymentMethod method,
                                               @RequestParam(required = false) Long customerId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "25") int size) {
        DateRanges.requireOrdered(from, to);
        return payments.list(ctx, from, to, method, customerId, Math.max(page, 0),
                Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/payments/{paymentId}")
    @RequirePermission(Permission.BILLING_VIEW)
    PaymentResponse getPayment(BusinessContext ctx, @PathVariable long paymentId) {
        return payments.get(ctx, paymentId);
    }

    @PostMapping("/payments/{paymentId}/void")
    @RequirePermission(Permission.BILLING_VOID)
    PaymentResponse voidPayment(BusinessContext ctx, @PathVariable long paymentId, @Valid @RequestBody ReasonBody body) {
        return payments.voidPayment(ctx, paymentId, body.reason());
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.BILLING_VOID)
    PaymentResponse refund(BusinessContext ctx, @PathVariable long paymentId, @Valid @RequestBody RefundBody body,
                           @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "refund:payment:" + paymentId, body,
                PaymentResponse.class, () -> payments.refund(ctx, paymentId, body.amount(), body.method(), body.reason()));
    }
}
