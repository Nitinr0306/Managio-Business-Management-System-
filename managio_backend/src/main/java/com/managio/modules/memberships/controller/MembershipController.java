package com.managio.modules.memberships.controller;

import com.managio.modules.memberships.service.MembershipService;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.billing.service.PaymentService.PaymentInput;
import com.managio.modules.memberships.repository.MembershipQueries.MembershipRow;
import com.managio.modules.memberships.repository.MembershipQueries.View;
import com.managio.modules.memberships.service.MembershipService.MembershipResponse;
import com.managio.modules.memberships.service.MembershipService.RenewRequest;
import com.managio.modules.memberships.service.MembershipService.StartRequest;
import com.managio.platform.persistence.IdempotentRequests;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequireModule;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.platform.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
@RequireModule(Module.MEMBERSHIPS)
@RequiredArgsConstructor
class MembershipController {

    private final MembershipService memberships;
    private final IdempotentRequests idempotency;

    /** Collect the first invoice straight away. Omit to leave it unpaid. */
    record PayNowBody(BigDecimal amount, @NotNull(message = "Choose how it was paid") PaymentMethod method,
                      @Size(max = 100) String reference) {
        PaymentInput toInput() {
            return new PaymentInput(amount, method, null, reference, null);
        }
    }

    record StartBody(@NotNull(message = "Choose the customer") Long customerId,
                     @NotNull(message = "Choose the plan") Long offeringId, LocalDate startsOn, BigDecimal discount,
                     @Valid PayNowBody payNow) {
        StartRequest toRequest() {
            return new StartRequest(customerId, offeringId, startsOn, discount, payNow == null ? null : payNow.toInput());
        }
    }

    record RenewBody(Long offeringId, BigDecimal discount, @Valid PayNowBody payNow) {
        RenewRequest toRequest() {
            return new RenewRequest(offeringId, discount, payNow == null ? null : payNow.toInput());
        }
    }

    record CancelBody(@Size(max = 300) String reason) {
    }

    @GetMapping("/memberships")
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    PageResponse<MembershipRow> list(BusinessContext ctx, @RequestParam(defaultValue = "ALL") View view,
                                     @RequestParam(defaultValue = "false") boolean needsRenewal,
                                     @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return memberships.list(ctx, view, needsRenewal, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/memberships/{membershipId}")
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    MembershipResponse get(BusinessContext ctx, @PathVariable long membershipId) {
        return memberships.get(ctx, membershipId);
    }

    @GetMapping("/customers/{customerId}/memberships")
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    List<MembershipResponse> forCustomer(BusinessContext ctx, @PathVariable long customerId) {
        return memberships.forCustomer(ctx, customerId);
    }

    @PostMapping("/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.MEMBERSHIPS_MANAGE)
    MembershipResponse start(BusinessContext ctx, @Valid @RequestBody StartBody body, @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "membership", body, MembershipResponse.class,
                () -> memberships.start(ctx, body.toRequest()));
    }

    @PostMapping("/memberships/{membershipId}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.MEMBERSHIPS_MANAGE)
    MembershipResponse renew(BusinessContext ctx, @PathVariable long membershipId, @Valid @RequestBody RenewBody body,
                             @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "renewal:" + membershipId, body,
                MembershipResponse.class, () -> memberships.renew(ctx, membershipId, body.toRequest()));
    }

    @PostMapping("/memberships/{membershipId}/cancel")
    @RequirePermission(Permission.MEMBERSHIPS_MANAGE)
    MembershipResponse cancel(BusinessContext ctx, @PathVariable long membershipId, @Valid @RequestBody CancelBody body) {
        return memberships.cancel(ctx, membershipId, body.reason());
    }
}
