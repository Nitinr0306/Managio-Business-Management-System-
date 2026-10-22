package com.managio.modules.customers.repository;

import com.managio.modules.memberships.domain.MembershipState;
import com.managio.platform.web.Nullable;
import com.managio.platform.persistence.SqlText;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model for customer lists, search and counts. One query computes membership coverage (MEM-8) and
 * balance due (BIL-10) for every customer on the page, so lists never run a query per row.
 */
@Component
@RequiredArgsConstructor
public class CustomerQueries {

    /** Which customers a list shows. */
    public enum View {
        ACTIVE("c.status = 'ACTIVE'"),
        ENDING_SOON("c.status = 'ACTIVE' AND cov.covered_today AND cov.covered_until < CAST(:today AS date) + 7"),
        LAPSED("c.status = 'ACTIVE' AND cov.covered_until IS NULL AND cov.last_ended IS NOT NULL"),
        NO_PLAN("c.status = 'ACTIVE' AND cov.customer_id IS NULL"),
        WITH_DUES("c.status = 'ACTIVE' AND dues.due > 0"),
        ARCHIVED("c.status = 'ARCHIVED'"),
        ALL("TRUE");

        private final String condition;

        View(String condition) {
            this.condition = condition;
        }
    }

    /** Allowed sort orders; the client picks a key, never raw SQL. */
    public enum Sort {
        NAME("lower(c.full_name), c.id"),
        NUMBER("c.number DESC"),
        NEWEST("c.created_at DESC, c.id DESC"),
        ENDING("cov.covered_until NULLS LAST, lower(c.full_name)"),
        DUES("dues.due DESC NULLS LAST, lower(c.full_name)");

        private final String orderBy;

        Sort(String orderBy) {
            this.orderBy = orderBy;
        }
    }

    public record CustomerRow(long id, int number, String fullName, @Nullable String phone, @Nullable String email,
                              String status, LocalDate joinedOn, MembershipState membershipState,
                              @Nullable String planName, @Nullable LocalDate coveredUntil,
                              @Nullable LocalDate lastEnded, BigDecimal balanceDue) {
    }

    public record Counts(long all, long active, long endingSoon, long lapsed, long noPlan, long withDues, long archived) {
    }

    /** One customer's membership status and money position. */
    public record Standing(MembershipState membershipState, @Nullable LocalDate coveredUntil, BigDecimal balanceDue,
                           BigDecimal upcoming) {
    }

    private static final String WITH = """
            WITH cov AS (
                SELECT customer_id,
                       max(ends_on) FILTER (WHERE ends_on >= :today) AS covered_until,
                       coalesce(bool_or(starts_on <= :today AND ends_on >= :today), false) AS covered_today,
                       max(ends_on) FILTER (WHERE ends_on < :today) AS last_ended
                FROM memberships
                WHERE business_id = :businessId AND status = 'ACTIVE'
                GROUP BY customer_id),
            paid AS (
                SELECT invoice_id, sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END) AS amount
                FROM payments
                WHERE business_id = :businessId AND voided_at IS NULL
                GROUP BY invoice_id),
            dues AS (
                SELECT i.customer_id, sum(i.total - coalesce(paid.amount, 0)) AS due
                FROM invoices i LEFT JOIN paid ON paid.invoice_id = i.id
                WHERE i.business_id = :businessId AND i.status = 'ISSUED' AND i.due_on <= :today
                  AND i.customer_id IS NOT NULL
                GROUP BY i.customer_id)
            """;

    private static final String FROM = """
            FROM customers c
            LEFT JOIN cov ON cov.customer_id = c.id
            LEFT JOIN dues ON dues.customer_id = c.id
            WHERE c.business_id = :businessId
            """;

    private static final String GROUP_FILTER = """
             AND EXISTS (SELECT 1 FROM customer_group_members gm WHERE gm.customer_id = c.id AND gm.group_id = :groupId)
            """;

    private final JdbcClient jdbc;

    public PageResponse<CustomerRow> list(long businessId, LocalDate today, View view, String search, Long groupId, Sort sort,
                                   int page, int size) {
        String where = " AND " + view.condition + searchClause(search) + (groupId == null ? "" : GROUP_FILTER);
        long total = bind(jdbc.sql(WITH + "SELECT count(*) " + FROM + where), businessId, today, search, groupId)
                .query(Long.class).single();
        List<CustomerRow> rows = bind(jdbc.sql(WITH + """
                        SELECT c.id, c.number, c.full_name, c.phone, c.email, c.status, c.joined_on,
                               coalesce(cov.covered_today, false) AS covered_today, cov.covered_until, cov.last_ended,
                               coalesce(dues.due, 0) AS due,
                               (SELECT o.name FROM memberships m JOIN offerings o ON o.id = m.offering_id
                                WHERE m.customer_id = c.id AND m.status = 'ACTIVE'
                                ORDER BY m.ends_on DESC LIMIT 1) AS plan_name
                        """ + FROM + where + " ORDER BY " + sort.orderBy + " LIMIT :limit OFFSET :offset"),
                businessId, today, search, groupId)
                .param("limit", size)
                .param("offset", page * size)
                .query((rs, i) -> row(rs, today))
                .list();
        return PageResponse.of(rows, page, size, total);
    }

    public Counts counts(long businessId, LocalDate today) {
        return jdbc.sql(WITH + """
                        SELECT count(*) AS all_count,
                               count(*) FILTER (WHERE %s) AS active,
                               count(*) FILTER (WHERE %s) AS ending_soon,
                               count(*) FILTER (WHERE %s) AS lapsed,
                               count(*) FILTER (WHERE %s) AS no_plan,
                               count(*) FILTER (WHERE %s) AS with_dues,
                               count(*) FILTER (WHERE %s) AS archived
                        """.formatted(View.ACTIVE.condition, View.ENDING_SOON.condition, View.LAPSED.condition,
                        View.NO_PLAN.condition, View.WITH_DUES.condition, View.ARCHIVED.condition) + FROM)
                .param("businessId", businessId)
                .param("today", today)
                .query((rs, i) -> new Counts(rs.getLong("all_count"), rs.getLong("active"), rs.getLong("ending_soon"),
                        rs.getLong("lapsed"), rs.getLong("no_plan"), rs.getLong("with_dues"), rs.getLong("archived")))
                .single();
    }

    public Standing standing(long businessId, long customerId, LocalDate today) {
        return jdbc.sql(WITH + """
                        SELECT coalesce(cov.covered_today, false) AS covered_today, cov.covered_until, cov.last_ended,
                               coalesce(dues.due, 0) AS due,
                               (SELECT coalesce(sum(i.total - coalesce(paid.amount, 0)), 0)
                                FROM invoices i LEFT JOIN paid ON paid.invoice_id = i.id
                                WHERE i.customer_id = c.id AND i.status = 'ISSUED' AND i.due_on > :today) AS upcoming
                        """ + FROM + " AND c.id = :customerId")
                .param("businessId", businessId)
                .param("today", today)
                .param("customerId", customerId)
                .query((rs, i) -> new Standing(
                        MembershipState.of(rs.getBoolean("covered_today"), date(rs, "covered_until"),
                                date(rs, "last_ended"), today),
                        date(rs, "covered_until"), rs.getBigDecimal("due"), rs.getBigDecimal("upcoming")))
                .single();
    }

    /**
     * Matches name, email or customer number. "#12" means customer number 12 only. Phone numbers are matched
     * by their digits once at least three are typed, so "2" doesn't match every phone containing a 2.
     */
    private static String searchClause(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        if (search.trim().startsWith("#")) {
            return " AND CAST(c.number AS text) = :term";
        }
        String phoneMatch = digits(search).length() >= 3 ? " OR c.phone LIKE :digits" : "";
        return " AND (lower(c.full_name) LIKE :like OR lower(c.email) LIKE :like OR CAST(c.number AS text) = :term"
                + phoneMatch + ")";
    }

    private static String digits(String search) {
        return search.replaceAll("[^0-9]", "");
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, long businessId, LocalDate today,
                                                 String search, Long groupId) {
        spec = spec.param("businessId", businessId).param("today", today);
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            spec = spec.param("like", SqlText.containing(term))
                    .param("term", term.replaceFirst("^#", ""));
            if (digits(search).length() >= 3 && !search.trim().startsWith("#")) {
                spec = spec.param("digits", "%" + digits(search) + "%");
            }
        }
        if (groupId != null) {
            spec = spec.param("groupId", groupId);
        }
        return spec;
    }

    private static CustomerRow row(ResultSet rs, LocalDate today) throws SQLException {
        LocalDate coveredUntil = date(rs, "covered_until");
        LocalDate lastEnded = date(rs, "last_ended");
        return new CustomerRow(rs.getLong("id"), rs.getInt("number"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), rs.getString("status"), date(rs, "joined_on"),
                MembershipState.of(rs.getBoolean("covered_today"), coveredUntil, lastEnded, today),
                rs.getString("plan_name"), coveredUntil, lastEnded, rs.getBigDecimal("due"));
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }
}
