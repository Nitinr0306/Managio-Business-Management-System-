package com.managio.modules.expenses.service;

import com.managio.modules.expenses.domain.Expense;
import com.managio.modules.expenses.domain.ExpenseCategory;
import com.managio.modules.expenses.repository.ExpenseRepository;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.daybook.service.DayLock;
import com.managio.platform.util.Money;
import com.managio.platform.persistence.Persistence;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Recording, voiding and listing expenses (EXP-1, EXP-2). */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final ExpenseCategoryService categories;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final DayLock dayLock;
    private final Persistence persistence;
    private final Clock clock;

    /** What is entered for an expense. */
    public record ExpenseInput(long categoryId, BigDecimal amount, LocalDate spentOn, PaymentMethod method, String payee,
                               String description, String reference) {
    }

    public record CategoryRef(long id, String name) {
    }

    public record ExpenseResponse(long id, CategoryRef category, BigDecimal amount, LocalDate spentOn,
                                  PaymentMethod method, @Nullable String payee, @Nullable String description,
                                  @Nullable String reference, @Nullable Long payrollEntryId, @Nullable Long requestId,
                                  @Nullable String recordedBy, boolean voided, @Nullable String voidReason,
                                  Instant createdAt) {
    }

    public record CategoryTotal(long categoryId, String category, BigDecimal total) {
    }

    public record ExpenseSummary(BigDecimal total, List<CategoryTotal> byCategory) {
    }

    /**
     * Records an expense. The caller has already checked who may do this: expenses.record for direct entry, or
     * requests.approve when an expense request is approved, or payroll.manage for salaries.
     */
    @Transactional
    public ExpenseResponse create(BusinessContext ctx, ExpenseInput input, Long requestId, Long payrollEntryId) {
        ExpenseCategory category = categories.get(ctx.businessId(), input.categoryId());
        if (category.isArchived() && payrollEntryId == null) {
            throw ApiException.rule("category.archived", category.getName() + " is archived. Choose another category.");
        }
        BigDecimal amount = Money.of(input.amount());
        if (amount.signum() <= 0) {
            throw ApiException.invalidField("amount", "Enter an amount greater than zero");
        }
        LocalDate spentOn = input.spentOn() != null ? input.spentOn() : ctx.today(clock);
        if (spentOn.isAfter(ctx.today(clock))) {
            throw ApiException.invalidField("spentOn", "The date can't be in the future");
        }
        dayLock.requireOpen(ctx.businessId(), spentOn, input.method() == PaymentMethod.CASH);
        Expense expense = expenses.save(Expense.record(ctx.businessId(), category.getId(), amount, spentOn, input.method(),
                Normalize.text(input.payee()), Normalize.text(input.description()), Normalize.text(input.reference()),
                payrollEntryId, requestId, ctx.userId()));
        activity.record(ctx, "EXPENSE_RECORDED", "EXPENSE", expense.getId(), "Recorded " + Money.inr(amount) + " for "
                + category.getName() + (expense.getPayee() == null ? "" : " to " + expense.getPayee()));
        return get(ctx, expense.getId());
    }

    /** EXP-2: an expense entered by mistake is voided with a reason. Salary expenses are voided here too. */
    @Transactional
    public ExpenseResponse voidExpense(BusinessContext ctx, long expenseId, String reason) {
        Expense expense = expenses.findByIdAndBusinessId(expenseId, ctx.businessId())
                .orElseThrow(() -> ApiException.notFound("Expense"));
        if (expense.isVoid()) {
            throw ApiException.rule("expense.already_void", "This expense was already voided.");
        }
        String clean = Normalize.text(reason);
        if (clean == null) {
            throw ApiException.invalidField("reason", "Say why, so the activity log makes sense later");
        }
        dayLock.requireOpen(ctx.businessId(), expense.getSpentOn(), expense.getMethod() == PaymentMethod.CASH);
        expense.voidExpense(clock.instant(), clean.length() > 300 ? clean.substring(0, 300) : clean);
        activity.record(ctx, "EXPENSE_VOIDED", "EXPENSE", expenseId, "Voided expense of " + Money.inr(expense.getAmount())
                + ": " + clean);
        return get(ctx, expenseId);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(BusinessContext ctx, long expenseId) {
        persistence.flush();
        return jdbc.sql(SELECT + " WHERE e.business_id = :businessId AND e.id = :id")
                .param("businessId", ctx.businessId()).param("id", expenseId)
                .query((rs, i) -> map(rs)).optional().orElseThrow(() -> ApiException.notFound("Expense"));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(long businessId, LocalDate from, LocalDate to, Long categoryId, String q,
                                              boolean includeVoid, int page, int size) {
        String where = " WHERE e.business_id = :businessId" + (includeVoid ? "" : " AND e.voided_at IS NULL")
                + (from == null ? "" : " AND e.spent_on >= :from") + (to == null ? "" : " AND e.spent_on <= :to")
                + (categoryId == null ? "" : " AND e.category_id = :categoryId")
                + (q == null || q.isBlank() ? "" : " AND (lower(e.payee) LIKE :like OR lower(e.description) LIKE :like)");
        long total = bind(jdbc.sql("SELECT count(*) FROM expenses e" + where), businessId, from, to, categoryId, q)
                .query(Long.class).single();
        List<ExpenseResponse> rows = bind(jdbc.sql(SELECT + where + " ORDER BY e.spent_on DESC, e.id DESC LIMIT :limit OFFSET :offset"),
                businessId, from, to, categoryId, q).param("limit", size).param("offset", page * size)
                .query((rs, i) -> map(rs)).list();
        return PageResponse.of(rows, page, size, total);
    }

    /** Totals by category for a period; voided expenses are excluded. */
    @Transactional(readOnly = true)
    public ExpenseSummary summary(long businessId, LocalDate from, LocalDate to) {
        List<CategoryTotal> byCategory = bind(jdbc.sql("""
                        SELECT c.id, c.name, sum(e.amount) AS total
                        FROM expenses e JOIN expense_categories c ON c.id = e.category_id
                        WHERE e.business_id = :businessId AND e.voided_at IS NULL"""
                        + (from == null ? "" : " AND e.spent_on >= :from") + (to == null ? "" : " AND e.spent_on <= :to")
                        + " GROUP BY c.id, c.name ORDER BY sum(e.amount) DESC"), businessId, from, to, null, null)
                .query((rs, i) -> new CategoryTotal(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("total"))).list();
        BigDecimal total = byCategory.stream().map(CategoryTotal::total).reduce(Money.ZERO, BigDecimal::add);
        return new ExpenseSummary(total, byCategory);
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, long businessId, LocalDate from,
                                                 LocalDate to, Long categoryId, String q) {
        spec = spec.param("businessId", businessId);
        if (from != null) {
            spec = spec.param("from", from);
        }
        if (to != null) {
            spec = spec.param("to", to);
        }
        if (categoryId != null) {
            spec = spec.param("categoryId", categoryId);
        }
        if (q != null && !q.isBlank()) {
            String term = q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            spec = spec.param("like", "%" + term + "%");
        }
        return spec;
    }

    private static final String SELECT = """
            SELECT e.id, e.category_id, c.name AS category_name, e.amount, e.spent_on, e.method, e.payee, e.description,
                   e.reference, e.payroll_entry_id, e.request_id, u.full_name AS recorded_by, e.voided_at, e.void_reason,
                   e.created_at
            FROM expenses e
            JOIN expense_categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.recorded_by_user_id""";

    private static ExpenseResponse map(ResultSet rs) throws SQLException {
        return new ExpenseResponse(rs.getLong("id"), new CategoryRef(rs.getLong("category_id"), rs.getString("category_name")),
                rs.getBigDecimal("amount"), rs.getObject("spent_on", LocalDate.class), PaymentMethod.valueOf(rs.getString("method")),
                rs.getString("payee"), rs.getString("description"), rs.getString("reference"),
                (Long) rs.getObject("payroll_entry_id"), (Long) rs.getObject("request_id"), rs.getString("recorded_by"),
                rs.getTimestamp("voided_at") != null, rs.getString("void_reason"), rs.getTimestamp("created_at").toInstant());
    }
}
