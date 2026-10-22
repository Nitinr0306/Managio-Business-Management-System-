package com.managio.modules.payroll.service;

import com.managio.modules.payroll.domain.PayrollEntry;
import com.managio.modules.payroll.repository.PayrollEntryRepository;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.expenses.service.ExpenseCategoryService;
import com.managio.modules.expenses.service.ExpenseService;
import com.managio.modules.expenses.service.ExpenseService.ExpenseInput;
import com.managio.platform.util.Money;
import com.managio.platform.persistence.Persistence;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Monthly salaries (PAY-1, PAY-2). Each payment is a Salaries expense linked to the month's entry. */
@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final DateTimeFormatter MONTH_NAME = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final PayrollEntryRepository entries;
    private final ExpenseService expenseService;
    private final ExpenseCategoryService categories;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final Persistence persistence;
    private final Clock clock;

    public enum State { UNPAID, PARTLY_PAID, PAID }

    public record PayrollRow(long id, long teamMemberId, String fullName, @Nullable String title, LocalDate period,
                             BigDecimal amount, BigDecimal paid, BigDecimal remaining, State state,
                             @Nullable String notes) {
    }

    /** What is entered to pay a salary; {@code amount} null means everything still owed. */
    public record PayInput(BigDecimal amount, PaymentMethod method, LocalDate paidOn, String notes) {
    }

    /** PAY-1: one entry per active team member with a salary, once per person and month. Safe to run again. */
    @Transactional
    public List<PayrollRow> prepare(BusinessContext ctx, LocalDate anyDayInMonth) {
        LocalDate period = monthStart(ctx, anyDayInMonth);
        int created = jdbc.sql("""
                        INSERT INTO payroll_entries (business_id, team_member_id, period, amount, created_at)
                        SELECT tm.business_id, tm.id, :period, tm.monthly_salary, now()
                        FROM team_members tm
                        WHERE tm.business_id = :businessId AND tm.status = 'ACTIVE' AND tm.monthly_salary IS NOT NULL
                          AND tm.joined_on < :nextPeriod
                        ON CONFLICT (team_member_id, period) DO NOTHING""")
                .param("businessId", ctx.businessId()).param("period", period).param("nextPeriod", period.plusMonths(1))
                .update();
        if (created > 0) {
            activity.record(ctx, "PAYROLL_PREPARED", "PAYROLL", null,
                    "Prepared " + MONTH_NAME.format(period) + " payroll for " + created + " people");
        }
        return list(ctx, period);
    }

    @Transactional(readOnly = true)
    public List<PayrollRow> list(BusinessContext ctx, LocalDate anyDayInMonth) {
        LocalDate period = anyDayInMonth.withDayOfMonth(1);
        return jdbc.sql(SELECT + " WHERE p.business_id = :businessId AND p.period = :period ORDER BY lower(u.full_name)")
                .param("businessId", ctx.businessId()).param("period", period)
                .query((rs, i) -> row(rs)).list();
    }

    /** PAY-2: pays all or part of a salary; the money goes out as a Salaries expense. */
    @Transactional
    public PayrollRow pay(BusinessContext ctx, long entryId, PayInput input) {
        PayrollEntry entry = entries.findForUpdate(entryId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Payroll entry"));
        PayrollRow current = get(ctx, entryId);
        BigDecimal amount = input.amount() == null ? current.remaining() : Money.of(input.amount());
        if (amount.signum() <= 0) {
            throw ApiException.invalidField("amount", current.remaining().signum() <= 0
                    ? "This salary is already paid" : "Enter an amount greater than zero");
        }
        if (amount.compareTo(current.remaining()) > 0) {
            throw ApiException.invalidField("amount", "Only " + Money.inr(current.remaining()) + " is left to pay");
        }
        expenseService.create(ctx, new ExpenseInput(categories.salaries(ctx.businessId()).getId(), amount, input.paidOn(),
                input.method(), current.fullName(), "Salary for " + MONTH_NAME.format(entry.getPeriod())
                + (Normalize.text(input.notes()) == null ? "" : " (" + Normalize.text(input.notes()) + ")"), null),
                null, entryId);
        return get(ctx, entryId);
    }

    /** A bonus or deduction changes what is owed, but never below what was already paid. */
    @Transactional
    public PayrollRow adjust(BusinessContext ctx, long entryId, BigDecimal amountInput, String notes) {
        PayrollEntry entry = entries.findForUpdate(entryId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Payroll entry"));
        BigDecimal amount = Money.of(amountInput);
        PayrollRow current = get(ctx, entryId);
        if (amount.signum() < 0 || amount.compareTo(current.paid()) < 0) {
            throw ApiException.invalidField("amount", "The salary can't be less than the " + Money.inr(current.paid()) + " already paid");
        }
        entry.adjust(amount, Normalize.text(notes));
        activity.record(ctx, "PAYROLL_ADJUSTED", "PAYROLL", entryId, "Set " + current.fullName() + "'s "
                + MONTH_NAME.format(entry.getPeriod()) + " salary to " + Money.inr(amount));
        return get(ctx, entryId);
    }

    @Transactional(readOnly = true)
    public PayrollRow get(BusinessContext ctx, long entryId) {
        persistence.flush();
        return jdbc.sql(SELECT + " WHERE p.business_id = :businessId AND p.id = :id")
                .param("businessId", ctx.businessId()).param("id", entryId)
                .query((rs, i) -> row(rs)).optional().orElseThrow(() -> ApiException.notFound("Payroll entry"));
    }

    /** Salaries can only be prepared for the current month or earlier. */
    private LocalDate monthStart(BusinessContext ctx, LocalDate anyDay) {
        LocalDate period = anyDay.withDayOfMonth(1);
        if (period.isAfter(ctx.today(clock).withDayOfMonth(1))) {
            throw ApiException.invalidField("month", "Payroll can't be prepared for a future month");
        }
        return period;
    }

    private static final String SELECT = """
            SELECT p.id, p.team_member_id, u.full_name, tm.title, p.period, p.amount, p.notes,
                   coalesce((SELECT sum(e.amount) FROM expenses e WHERE e.payroll_entry_id = p.id AND e.voided_at IS NULL), 0) AS paid
            FROM payroll_entries p
            JOIN team_members tm ON tm.id = p.team_member_id
            JOIN users u ON u.id = tm.user_id""";

    private static PayrollRow row(ResultSet rs) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal paid = rs.getBigDecimal("paid");
        BigDecimal remaining = amount.subtract(paid).max(BigDecimal.ZERO);
        State state = paid.compareTo(amount) >= 0 ? State.PAID : paid.signum() > 0 ? State.PARTLY_PAID : State.UNPAID;
        return new PayrollRow(rs.getLong("id"), rs.getLong("team_member_id"), rs.getString("full_name"), rs.getString("title"),
                rs.getObject("period", LocalDate.class), amount, paid, remaining, state, rs.getString("notes"));
    }
}
