package com.managio.modules.expenses.controller;

import com.managio.modules.expenses.service.ExpenseCategoryService;
import com.managio.modules.expenses.service.ExpenseService;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.expenses.service.ExpenseCategoryService.CategoryResponse;
import com.managio.modules.expenses.service.ExpenseService.ExpenseInput;
import com.managio.modules.expenses.service.ExpenseService.ExpenseResponse;
import com.managio.modules.expenses.service.ExpenseService.ExpenseSummary;
import com.managio.platform.persistence.IdempotentRequests;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.platform.web.DateRanges;
import com.managio.platform.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
class ExpenseController {

    private final ExpenseService expenses;
    private final ExpenseCategoryService categories;
    private final IdempotentRequests idempotency;

    record ExpenseBody(@NotNull(message = "Choose a category") Long categoryId,
                       @NotNull(message = "Enter an amount") BigDecimal amount, LocalDate spentOn,
                       @NotNull(message = "Choose how it was paid") PaymentMethod method,
                       @Size(max = 150) String payee, @Size(max = 500) String description, @Size(max = 100) String reference) {
        ExpenseInput toInput() {
            return new ExpenseInput(categoryId, amount, spentOn, method, payee, description, reference);
        }
    }

    record ReasonBody(@NotNull(message = "Say why") @Size(max = 300) String reason) {
    }

    record CategoryBody(@NotBlank(message = "Enter a name") @Size(max = 60, message = "Use 60 characters or fewer") String name,
                        boolean archived) {
    }

    @GetMapping("/expenses")
    @RequirePermission(Permission.EXPENSES_VIEW)
    PageResponse<ExpenseResponse> list(BusinessContext ctx,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "false") boolean includeVoid,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        DateRanges.requireOrdered(from, to);
        return expenses.list(ctx.businessId(), from, to, categoryId, q, includeVoid, Math.max(page, 0),
                Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/expenses/summary")
    @RequirePermission(Permission.EXPENSES_VIEW)
    ExpenseSummary summary(BusinessContext ctx,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DateRanges.requireOrdered(from, to);
        return expenses.summary(ctx.businessId(), from, to);
    }

    @GetMapping("/expenses/{expenseId}")
    @RequirePermission(Permission.EXPENSES_VIEW)
    ExpenseResponse get(BusinessContext ctx, @PathVariable long expenseId) {
        return expenses.get(ctx, expenseId);
    }

    /** EXP-1: direct entry needs expenses.record; everyone else raises a request instead. */
    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.EXPENSES_RECORD)
    ExpenseResponse create(BusinessContext ctx, @Valid @RequestBody ExpenseBody body, @RequestHeader(name = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        return idempotency.once(ctx.businessId(), ctx.userId(), idempotencyKey, "expense", body, ExpenseResponse.class,
                () -> expenses.create(ctx, body.toInput(), null, null));
    }

    @PostMapping("/expenses/{expenseId}/void")
    @RequirePermission(Permission.EXPENSES_RECORD)
    ExpenseResponse voidExpense(BusinessContext ctx, @PathVariable long expenseId, @Valid @RequestBody ReasonBody body) {
        return expenses.voidExpense(ctx, expenseId, body.reason());
    }

    /** Every team member may read the categories: raising an expense claim (REQ-1) needs one. */
    @GetMapping("/expense-categories")
    List<CategoryResponse> categories(BusinessContext ctx) {
        return categories.list(ctx.businessId());
    }

    @PostMapping("/expense-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.SETTINGS_MANAGE)
    CategoryResponse createCategory(BusinessContext ctx, @Valid @RequestBody CategoryBody body) {
        return categories.create(ctx, body.name());
    }

    @PatchMapping("/expense-categories/{categoryId}")
    @RequirePermission(Permission.SETTINGS_MANAGE)
    CategoryResponse updateCategory(BusinessContext ctx, @PathVariable long categoryId, @Valid @RequestBody CategoryBody body) {
        return categories.update(ctx, categoryId, body.name(), body.archived());
    }
}
