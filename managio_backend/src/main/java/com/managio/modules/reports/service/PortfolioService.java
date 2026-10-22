package com.managio.modules.reports.service;

import com.managio.platform.util.Money;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REP-2: one line per business the user works in, so an owner of several businesses sees where attention is
 * needed. Each figure is filled only where the user's role in that business allows it; the rest are null.
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final JdbcClient jdbc;
    private final ReportService reports;
    private final Clock clock;

    public record BusinessLine(long id, String code, String name, String type, boolean owner,
                               @Nullable BigDecimal revenueThisMonth, @Nullable BigDecimal expensesThisMonth,
                               @Nullable BigDecimal profitThisMonth, @Nullable Long endingThisWeek,
                               @Nullable BigDecimal overdueAmount, @Nullable Long pendingApprovals, long myOpenTasks) {
    }

    /** Totals add up only the businesses where the user may see reports. */
    public record Totals(int businessesCounted, BigDecimal revenueThisMonth, BigDecimal expensesThisMonth, BigDecimal profitThisMonth) {
    }

    public record Portfolio(LocalDate monthStart, List<BusinessLine> businesses, Totals totals) {
    }

    private record Membership(long businessId, long memberId, String code, String name, String type, boolean owner,
                              ZoneId zone, Set<Module> modules, Set<Permission> permissions) {
    }

    @Transactional(readOnly = true)
    public Portfolio portfolio(long userId) {
        List<Membership> memberships = jdbc.sql("""
                        SELECT b.id, b.code, b.name, b.type, b.timezone, b.modules, tm.id AS member_id, r.system_key, r.permissions
                        FROM team_members tm
                        JOIN businesses b ON b.id = tm.business_id
                        JOIN roles r ON r.id = tm.role_id
                        WHERE tm.user_id = :userId AND tm.status = 'ACTIVE' AND b.archived_at IS NULL
                        ORDER BY lower(b.name)""")
                .param("userId", userId)
                .query((rs, i) -> new Membership(rs.getLong("id"), rs.getLong("member_id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("type"), "OWNER".equals(rs.getString("system_key")), ZoneId.of(rs.getString("timezone")),
                        Module.fromNames(strings(rs.getArray("modules"))), Permission.fromKeys(strings(rs.getArray("permissions")))))
                .list();

        BigDecimal revenue = Money.ZERO;
        BigDecimal expenses = Money.ZERO;
        int counted = 0;
        LocalDate firstMonthStart = null;
        List<BusinessLine> lines = new java.util.ArrayList<>();
        for (Membership m : memberships) {
            LocalDate today = LocalDate.now(clock.withZone(m.zone()));
            LocalDate monthStart = today.withDayOfMonth(1);
            firstMonthStart = firstMonthStart == null ? monthStart : firstMonthStart;

            BigDecimal rev = null;
            BigDecimal exp = null;
            if (m.permissions().contains(Permission.REPORTS_VIEW)) {
                rev = sum("SELECT coalesce(sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END), 0) FROM payments "
                        + "WHERE business_id = :b AND paid_on BETWEEN :from AND :to AND voided_at IS NULL", m.businessId(), monthStart, today);
                exp = sum("SELECT coalesce(sum(amount), 0) FROM expenses WHERE business_id = :b AND spent_on BETWEEN :from AND :to AND voided_at IS NULL",
                        m.businessId(), monthStart, today);
                revenue = revenue.add(rev);
                expenses = expenses.add(exp);
                counted++;
            }
            Long ending = m.permissions().contains(Permission.CUSTOMERS_VIEW) && m.modules().contains(Module.MEMBERSHIPS)
                    ? reports.endingOn(m.businessId(), today, today.plusDays(6)) : null;
            BigDecimal overdue = m.permissions().contains(Permission.BILLING_VIEW)
                    ? reports.dues(m.businessId(), today).overdueAmount() : null;
            Long pending = m.permissions().contains(Permission.REQUESTS_APPROVE)
                    ? jdbc.sql("SELECT count(*) FROM requests WHERE business_id = :b AND status = 'PENDING' AND requested_by_member_id <> :m")
                    .param("b", m.businessId()).param("m", m.memberId()).query(Long.class).single() : null;
            long tasks = jdbc.sql("SELECT count(*) FROM tasks WHERE business_id = :b AND assignee_id = :m AND status IN ('OPEN', 'IN_PROGRESS')")
                    .param("b", m.businessId()).param("m", m.memberId()).query(Long.class).single();

            lines.add(new BusinessLine(m.businessId(), m.code(), m.name(), m.type(), m.owner(), rev, exp,
                    rev == null ? null : rev.subtract(exp), ending, overdue, pending, tasks));
        }
        return new Portfolio(firstMonthStart, lines, new Totals(counted, revenue, expenses, revenue.subtract(expenses)));
    }

    private BigDecimal sum(String sql, long businessId, LocalDate from, LocalDate to) {
        return jdbc.sql(sql).params(Map.of("b", businessId, "from", from, "to", to)).query(BigDecimal.class).single();
    }

    private static String[] strings(java.sql.Array array) throws java.sql.SQLException {
        return array == null ? new String[0] : (String[]) array.getArray();
    }
}
