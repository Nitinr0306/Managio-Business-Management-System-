package com.managio.modules.reports.service;

import com.managio.platform.util.Money;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read models for the Today view, the renewal runway and profit and loss (REP-1). Every figure is computed
 * from invoices, payments, expenses and memberships, so nothing here can drift from the records.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    /** A membership counts as "still to renew" until a later membership is renewed from it (MEM-5). */
    static final String UNRENEWED = "m.status = 'ACTIVE' AND c.status = 'ACTIVE' AND NOT EXISTS ("
            + "SELECT 1 FROM memberships r WHERE r.renewed_from_id = m.id AND r.status = 'ACTIVE')";

    private static final int MAX_RANGE_DAYS = 366;
    private static final int DAILY_SERIES_MAX_DAYS = 62;
    private static final int ENDING_SOON_DAYS = 7;
    private static final int LAPSED_LOOKBACK_DAYS = 30;

    private final JdbcClient jdbc;
    private final Clock clock;

    /** Sections the caller isn't allowed to see are null. */
    public record Today(LocalDate date, @Nullable Renewals renewals, @Nullable Dues dues, @Nullable Takings takings,
                        @Nullable Boolean dayClosed, @Nullable Long newCustomersThisMonth, MyTasks myTasks,
                        @Nullable Long pendingApprovals) {
    }

    public record Renewals(long endingToday, long endingThisWeek, long lapsedLast30Days) {
    }

    public record Dues(long openInvoices, BigDecimal openAmount, long overdueInvoices, BigDecimal overdueAmount) {
    }

    public record Takings(BigDecimal received, BigDecimal spent) {
    }

    public record MyTasks(long open, long overdue) {
    }

    public record RunwayDay(LocalDate date, long ending, BigDecimal value) {
    }

    public record Runway(LocalDate from, LocalDate to, List<RunwayDay> days, long lapsedLast30Days, BigDecimal lapsedValue) {
    }

    public record SeriesPoint(LocalDate bucket, BigDecimal revenue, BigDecimal expenses, BigDecimal profit) {
    }

    public record CategoryAmount(String category, BigDecimal amount) {
    }

    public record ProfitAndLoss(LocalDate from, LocalDate to, String granularity, BigDecimal revenue, BigDecimal expenses,
                                BigDecimal profit, Map<String, BigDecimal> revenueByMethod, List<CategoryAmount> expensesByCategory,
                                List<SeriesPoint> series) {
    }

    @Transactional(readOnly = true)
    public Today today(BusinessContext ctx) {
        long businessId = ctx.businessId();
        LocalDate today = ctx.today(clock);

        Renewals renewals = null;
        Long newCustomers = null;
        if (ctx.can(Permission.CUSTOMERS_VIEW)) {
            newCustomers = one(Long.class, "SELECT count(*) FROM customers WHERE business_id = :b AND joined_on >= :monthStart",
                    Map.of("b", businessId, "monthStart", today.withDayOfMonth(1)));
            if (ctx.has(Module.MEMBERSHIPS)) {
                renewals = new Renewals(endingOn(businessId, today, today), endingOn(businessId, today, today.plusDays(ENDING_SOON_DAYS - 1)),
                        lapsed(businessId, today));
            }
        }

        Dues dues = null;
        Takings takings = null;
        if (ctx.can(Permission.BILLING_VIEW)) {
            dues = dues(businessId, today);
        }
        if (ctx.can(Permission.BILLING_VIEW) && ctx.can(Permission.EXPENSES_VIEW)) {
            takings = new Takings(
                    one(BigDecimal.class, "SELECT coalesce(sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END), 0) FROM payments "
                            + "WHERE business_id = :b AND paid_on = :d AND voided_at IS NULL", Map.of("b", businessId, "d", today)),
                    one(BigDecimal.class, "SELECT coalesce(sum(amount), 0) FROM expenses WHERE business_id = :b AND spent_on = :d AND voided_at IS NULL",
                            Map.of("b", businessId, "d", today)));
        }

        Boolean dayClosed = ctx.can(Permission.DAYBOOK_CLOSE)
                ? one(Boolean.class, "SELECT EXISTS (SELECT 1 FROM day_closes WHERE business_id = :b AND business_date = :d)",
                Map.of("b", businessId, "d", today)) : null;

        MyTasks tasks = new MyTasks(
                one(Long.class, "SELECT count(*) FROM tasks WHERE business_id = :b AND assignee_id = :m AND status IN ('OPEN', 'IN_PROGRESS')",
                        Map.of("b", businessId, "m", ctx.memberId())),
                one(Long.class, "SELECT count(*) FROM tasks WHERE business_id = :b AND assignee_id = :m AND status IN ('OPEN', 'IN_PROGRESS') AND due_on < :d",
                        Map.of("b", businessId, "m", ctx.memberId(), "d", today)));

        Long pending = ctx.can(Permission.REQUESTS_APPROVE)
                ? one(Long.class, "SELECT count(*) FROM requests WHERE business_id = :b AND status = 'PENDING' AND requested_by_member_id <> :m",
                Map.of("b", businessId, "m", ctx.memberId())) : null;

        return new Today(today, renewals, dues, takings, dayClosed, newCustomers, tasks, pending);
    }

    /** Memberships ending on each of the next {@code days} days that nobody has renewed, plus those that lapsed recently. */
    @Transactional(readOnly = true)
    public Runway runway(BusinessContext ctx, int days) {
        if (days < 1 || days > 90) {
            throw ApiException.invalidField("days", "Choose between 1 and 90 days");
        }
        LocalDate from = ctx.today(clock);
        LocalDate to = from.plusDays(days - 1);
        Map<LocalDate, RunwayDay> found = new TreeMap<>();
        jdbc.sql("SELECT m.ends_on, count(*) AS n, coalesce(sum(m.price), 0) AS value "
                        + "FROM memberships m JOIN customers c ON c.id = m.customer_id "
                        + "WHERE m.business_id = :b AND m.ends_on BETWEEN :from AND :to AND " + UNRENEWED + " GROUP BY m.ends_on")
                .param("b", ctx.businessId()).param("from", from).param("to", to)
                .query((rs, i) -> new RunwayDay(rs.getObject("ends_on", LocalDate.class), rs.getLong("n"), rs.getBigDecimal("value")))
                .list().forEach(d -> found.put(d.date(), d));
        List<RunwayDay> series = new ArrayList<>(days);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            series.add(found.getOrDefault(d, new RunwayDay(d, 0, Money.ZERO)));
        }
        BigDecimal lapsedValue = one(BigDecimal.class, "SELECT coalesce(sum(m.price), 0) FROM memberships m JOIN customers c ON c.id = m.customer_id "
                        + "WHERE m.business_id = :b AND m.ends_on < :today AND m.ends_on >= :since AND " + UNRENEWED,
                Map.of("b", ctx.businessId(), "today", from, "since", from.minusDays(LAPSED_LOOKBACK_DAYS)));
        return new Runway(from, to, series, lapsed(ctx.businessId(), from), lapsedValue);
    }

    /** REP-1: cash basis. Revenue is payments minus refunds by paid date, expenses by spent date; void records are excluded. */
    @Transactional(readOnly = true)
    public ProfitAndLoss profitAndLoss(BusinessContext ctx, LocalDate from, LocalDate to) {
        LocalDate today = ctx.today(clock);
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : today;
        if (end.isBefore(start)) {
            throw ApiException.invalidField("to", "The end date can't be before the start date");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw ApiException.invalidField("to", "Choose a range of a year or less");
        }
        boolean daily = ChronoUnit.DAYS.between(start, end) < DAILY_SERIES_MAX_DAYS;
        String paidBucket = daily ? "paid_on" : "date_trunc('month', paid_on)::date";
        String spentBucket = daily ? "spent_on" : "date_trunc('month', spent_on)::date";
        long businessId = ctx.businessId();

        Map<LocalDate, BigDecimal[]> buckets = new TreeMap<>();
        jdbc.sql("SELECT " + paidBucket + " AS bucket, sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END) AS amount FROM payments "
                        + "WHERE business_id = :b AND paid_on BETWEEN :from AND :to AND voided_at IS NULL GROUP BY 1")
                .param("b", businessId).param("from", start).param("to", end)
                .query((rs, i) -> Map.entry(rs.getObject("bucket", LocalDate.class), rs.getBigDecimal("amount"))).list()
                .forEach(e -> buckets.computeIfAbsent(e.getKey(), k -> new BigDecimal[]{Money.ZERO, Money.ZERO})[0] = e.getValue());
        jdbc.sql("SELECT " + spentBucket + " AS bucket, sum(amount) AS amount FROM expenses "
                        + "WHERE business_id = :b AND spent_on BETWEEN :from AND :to AND voided_at IS NULL GROUP BY 1")
                .param("b", businessId).param("from", start).param("to", end)
                .query((rs, i) -> Map.entry(rs.getObject("bucket", LocalDate.class), rs.getBigDecimal("amount"))).list()
                .forEach(e -> buckets.computeIfAbsent(e.getKey(), k -> new BigDecimal[]{Money.ZERO, Money.ZERO})[1] = e.getValue());

        List<SeriesPoint> series = new ArrayList<>();
        BigDecimal revenue = Money.ZERO;
        BigDecimal expenses = Money.ZERO;
        for (Map.Entry<LocalDate, BigDecimal[]> e : buckets.entrySet()) {
            BigDecimal in = Money.of(e.getValue()[0]);
            BigDecimal out = Money.of(e.getValue()[1]);
            series.add(new SeriesPoint(e.getKey(), in, out, in.subtract(out)));
            revenue = revenue.add(in);
            expenses = expenses.add(out);
        }

        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        jdbc.sql("SELECT method, sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END) AS amount FROM payments "
                        + "WHERE business_id = :b AND paid_on BETWEEN :from AND :to AND voided_at IS NULL GROUP BY method ORDER BY 2 DESC")
                .param("b", businessId).param("from", start).param("to", end)
                .query((rs, i) -> Map.entry(rs.getString("method"), rs.getBigDecimal("amount"))).list()
                .forEach(e -> byMethod.put(e.getKey(), e.getValue()));
        List<CategoryAmount> byCategory = jdbc.sql("SELECT c.name, sum(e.amount) AS amount FROM expenses e JOIN expense_categories c ON c.id = e.category_id "
                        + "WHERE e.business_id = :b AND e.spent_on BETWEEN :from AND :to AND e.voided_at IS NULL GROUP BY c.name ORDER BY 2 DESC, c.name")
                .param("b", businessId).param("from", start).param("to", end)
                .query((rs, i) -> new CategoryAmount(rs.getString("name"), rs.getBigDecimal("amount"))).list();

        return new ProfitAndLoss(start, end, daily ? "DAY" : "MONTH", revenue, expenses, revenue.subtract(expenses), byMethod, byCategory, series);
    }

    Dues dues(long businessId, LocalDate today) {
        return jdbc.sql("""
                        WITH p AS (
                            SELECT invoice_id, sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END) AS paid
                            FROM payments WHERE business_id = :b AND voided_at IS NULL GROUP BY invoice_id)
                        SELECT count(*) FILTER (WHERE i.total > coalesce(p.paid, 0)) AS open_n,
                               coalesce(sum(i.total - coalesce(p.paid, 0)) FILTER (WHERE i.total > coalesce(p.paid, 0)), 0) AS open_amount,
                               count(*) FILTER (WHERE i.total > coalesce(p.paid, 0) AND i.due_on < :today) AS overdue_n,
                               coalesce(sum(i.total - coalesce(p.paid, 0)) FILTER (WHERE i.total > coalesce(p.paid, 0) AND i.due_on < :today), 0) AS overdue_amount
                        FROM invoices i LEFT JOIN p ON p.invoice_id = i.id
                        WHERE i.business_id = :b AND i.status = 'ISSUED'""")
                .param("b", businessId).param("today", today)
                .query((rs, i) -> new Dues(rs.getLong("open_n"), rs.getBigDecimal("open_amount"), rs.getLong("overdue_n"),
                        rs.getBigDecimal("overdue_amount")))
                .single();
    }

    long endingOn(long businessId, LocalDate from, LocalDate to) {
        return one(Long.class, "SELECT count(*) FROM memberships m JOIN customers c ON c.id = m.customer_id "
                        + "WHERE m.business_id = :b AND m.ends_on BETWEEN :from AND :to AND " + UNRENEWED,
                Map.of("b", businessId, "from", from, "to", to));
    }

    long lapsed(long businessId, LocalDate today) {
        return one(Long.class, "SELECT count(*) FROM memberships m JOIN customers c ON c.id = m.customer_id "
                        + "WHERE m.business_id = :b AND m.ends_on < :today AND m.ends_on >= :since AND " + UNRENEWED,
                Map.of("b", businessId, "today", today, "since", today.minusDays(LAPSED_LOOKBACK_DAYS)));
    }

    private <T> T one(Class<T> type, String sql, Map<String, ?> params) {
        return jdbc.sql(sql).params(params).query(type).single();
    }
}
