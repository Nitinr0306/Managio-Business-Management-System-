package com.managio.modules.payroll.controller;

import com.managio.modules.payroll.service.PayrollService;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.platform.persistence.IdempotentRequests;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequireModule;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.modules.payroll.service.PayrollService.PayInput;
import com.managio.modules.payroll.service.PayrollService.PayrollRow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Salaries. Needs the Payroll module and payroll.manage: nobody else sees what people earn (TEAM-9). */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/payroll")
@RequireModule(Module.PAYROLL)
@RequirePermission(Permission.PAYROLL_MANAGE)
@RequiredArgsConstructor
class PayrollController {

    private final PayrollService payroll;
    private final IdempotentRequests idempotency;
    private final Clock clock;

    record MonthBody(LocalDate month) {
    }

    record PayBody(BigDecimal amount, @NotNull(message = "Choose how it was paid") PaymentMethod method, LocalDate paidOn,
                   @Size(max = 200) String notes) {
    }

    record AdjustBody(@NotNull(message = "Enter the salary") BigDecimal amount, @Size(max = 300) String notes) {
    }

    @GetMapping
    List<PayrollRow> list(BusinessContext ctx,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return payroll.list(ctx, month != null ? month : ctx.today(clock));
    }

    @PostMapping("/prepare")
    List<PayrollRow> prepare(BusinessContext ctx, @RequestBody MonthBody body) {
        return payroll.prepare(ctx, body.month() != null ? body.month() : ctx.today(clock));
    }

    @PostMapping("/{entryId}/payments")
    PayrollRow pay(BusinessContext ctx, @PathVariable long entryId, @Valid @RequestBody PayBody body, @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "payroll-payment:" + entryId, body,
                PayrollRow.class,
                () -> payroll.pay(ctx, entryId, new PayInput(body.amount(), body.method(), body.paidOn(), body.notes())));
    }

    @PatchMapping("/{entryId}")
    PayrollRow adjust(BusinessContext ctx, @PathVariable long entryId, @Valid @RequestBody AdjustBody body) {
        return payroll.adjust(ctx, entryId, body.amount(), body.notes());
    }
}
