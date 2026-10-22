package com.managio.modules.requests.controller;

import com.managio.modules.requests.service.RequestService;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.requests.domain.ApprovalRequest.Details;
import com.managio.modules.requests.domain.ApprovalRequest.Status;
import com.managio.modules.requests.domain.ApprovalRequest.Type;
import com.managio.modules.requests.service.RequestService.RequestResponse;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.platform.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Anyone on the team may ask and see their own requests; approving needs requests.approve (REQ-1, REQ-2). */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/requests")
@RequiredArgsConstructor
class RequestController {

    private final RequestService requests;

    record RequestPayload(@NotNull(message = "Choose the kind of request") Type type,
                        @NotBlank(message = "Enter a short title") @Size(max = 180, message = "Use 180 characters or fewer") String title,
                        @Size(max = 4000) String details, BigDecimal amount, Long expenseCategoryId, LocalDate startsOn,
                        LocalDate endsOn, Long invoiceId) {
        Details toDetails() {
            return new Details(type, title, details, amount, expenseCategoryId, startsOn, endsOn, invoiceId);
        }
    }

    record ApproveBody(@Size(max = 500) String note, PaymentMethod method) {
    }

    record RejectBody(@NotBlank(message = "Say why, so they know what to change") @Size(max = 500) String note) {
    }

    @GetMapping
    PageResponse<RequestResponse> list(BusinessContext ctx, @RequestParam(required = false) Status status,
                                       @RequestParam(defaultValue = "false") boolean mine,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return requests.list(ctx, status, mine, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/pending-count")
    @RequirePermission(Permission.REQUESTS_APPROVE)
    Map<String, Long> pendingCount(BusinessContext ctx) {
        return Map.of("pending", requests.pendingCount(ctx));
    }

    @GetMapping("/{requestId}")
    RequestResponse get(BusinessContext ctx, @PathVariable long requestId) {
        return requests.get(ctx, requestId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RequestResponse create(BusinessContext ctx, @Valid @RequestBody RequestPayload body) {
        return requests.create(ctx, body.toDetails());
    }

    @PostMapping("/{requestId}/cancel")
    RequestResponse cancel(BusinessContext ctx, @PathVariable long requestId) {
        return requests.cancel(ctx, requestId);
    }

    @PostMapping("/{requestId}/approve")
    @RequirePermission(Permission.REQUESTS_APPROVE)
    RequestResponse approve(BusinessContext ctx, @PathVariable long requestId, @Valid @RequestBody ApproveBody body) {
        return requests.approve(ctx, requestId, body.note(), body.method());
    }

    @PostMapping("/{requestId}/reject")
    @RequirePermission(Permission.REQUESTS_APPROVE)
    RequestResponse reject(BusinessContext ctx, @PathVariable long requestId, @Valid @RequestBody RejectBody body) {
        return requests.reject(ctx, requestId, body.note());
    }
}
