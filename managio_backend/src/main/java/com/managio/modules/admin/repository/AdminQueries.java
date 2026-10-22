package com.managio.modules.admin.repository;

import com.managio.modules.admin.dto.AdminDtos.AccountStatus;
import com.managio.modules.admin.dto.AdminDtos.ActivationFunnel;
import com.managio.modules.admin.dto.AdminDtos.ActivityDay;
import com.managio.modules.admin.dto.AdminDtos.DatabaseStatus;
import com.managio.modules.admin.dto.AdminDtos.FeatureUsage;
import com.managio.modules.admin.dto.AdminDtos.KeyCount;
import com.managio.modules.admin.dto.AdminDtos.PlatformTotals;
import com.managio.modules.admin.dto.AdminDtos.PlatformUserRow;
import com.managio.modules.admin.dto.AdminDtos.SignInStatus;
import com.managio.modules.admin.dto.AdminDtos.SignupWeek;
import com.managio.modules.admin.dto.AdminDtos.TenantRow;
import com.managio.platform.persistence.SqlText;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Platform-wide read model for the console (D-028). Every query aggregates across businesses or reads business
 * and account metadata; none returns a business's customers, invoices, payments or amounts.
 */
@Repository
@RequiredArgsConstructor
public class AdminQueries {

    private static final Duration DAY = Duration.ofDays(1);

    private final JdbcClient jdbc;

    public PlatformTotals totals(Instant now) {
        return jdbc.sql("""
                        SELECT (SELECT count(*) FROM businesses WHERE archived_at IS NULL) AS businesses,
                               (SELECT count(*) FROM businesses WHERE archived_at IS NULL AND created_at >= :d30) AS new_businesses_30d,
                               (SELECT count(DISTINCT business_id) FROM activity_events WHERE created_at >= :d7) AS active_businesses_7d,
                               (SELECT count(DISTINCT business_id) FROM activity_events WHERE created_at >= :d30) AS active_businesses_30d,
                               (SELECT count(*) FROM businesses WHERE archived_at IS NOT NULL) AS archived_businesses,
                               (SELECT count(*) FROM users) AS users,
                               (SELECT count(*) FROM users WHERE created_at >= :d30) AS new_users_30d,
                               (SELECT count(*) FROM users WHERE email_verified_at IS NULL) AS unverified_users,
                               (SELECT count(DISTINCT subject_id) FROM refresh_tokens WHERE subject_type = 'USER' AND created_at >= :d1) AS active_users_1d,
                               (SELECT count(DISTINCT subject_id) FROM refresh_tokens WHERE subject_type = 'USER' AND created_at >= :d7) AS active_users_7d,
                               (SELECT count(DISTINCT subject_id) FROM refresh_tokens WHERE subject_type = 'USER' AND created_at >= :d30) AS active_users_30d,
                               (SELECT count(*) FROM team_members WHERE status = 'ACTIVE') AS team_seats,
                               (SELECT count(*) FROM customers WHERE status = 'ACTIVE') AS customers,
                               (SELECT count(*) FROM customers WHERE created_at >= :d30) AS new_customers_30d,
                               (SELECT count(*) FROM customers WHERE status = 'ACTIVE' AND portal_enabled) AS portal_customers,
                               (SELECT count(DISTINCT subject_id) FROM refresh_tokens WHERE subject_type = 'CUSTOMER' AND created_at >= :d30) AS active_portal_30d""")
                .param("d1", ago(now, 1)).param("d7", ago(now, 7)).param("d30", ago(now, 30))
                .query((rs, i) -> new PlatformTotals(rs.getLong("businesses"), rs.getLong("new_businesses_30d"),
                        rs.getLong("active_businesses_7d"), rs.getLong("active_businesses_30d"),
                        rs.getLong("archived_businesses"), rs.getLong("users"), rs.getLong("new_users_30d"),
                        rs.getLong("unverified_users"), rs.getLong("active_users_1d"), rs.getLong("active_users_7d"),
                        rs.getLong("active_users_30d"), rs.getLong("team_seats"), rs.getLong("customers"),
                        rs.getLong("new_customers_30d"), rs.getLong("portal_customers"), rs.getLong("active_portal_30d")))
                .single();
    }

    /** New accounts and businesses per week (weeks start on Monday in {@code zone}), oldest first. */
    public List<SignupWeek> growth(Instant now, String zone, int weeks) {
        return jdbc.sql("""
                        SELECT w.week_start::date AS week_start,
                               (SELECT count(*) FROM users u
                                WHERE u.created_at >= w.week_start AT TIME ZONE :zone
                                  AND u.created_at < (w.week_start + interval '1 week') AT TIME ZONE :zone) AS users,
                               (SELECT count(*) FROM businesses b
                                WHERE b.created_at >= w.week_start AT TIME ZONE :zone
                                  AND b.created_at < (w.week_start + interval '1 week') AT TIME ZONE :zone) AS businesses
                        FROM generate_series(date_trunc('week', CAST(:now AS timestamptz) AT TIME ZONE :zone) - (:weeks - 1) * interval '1 week',
                                             date_trunc('week', CAST(:now AS timestamptz) AT TIME ZONE :zone),
                                             interval '1 week') AS w(week_start)
                        ORDER BY w.week_start""")
                .param("now", Timestamp.from(now)).param("zone", zone).param("weeks", weeks)
                .query((rs, i) -> new SignupWeek(rs.getObject("week_start", LocalDate.class), rs.getLong("users"),
                        rs.getLong("businesses")))
                .list();
    }

    /** Things recorded, businesses that recorded them and people who used the app, per day in {@code zone}. */
    public List<ActivityDay> activity(Instant now, String zone, int days) {
        return jdbc.sql("""
                        SELECT d.day::date AS day,
                               (SELECT count(*) FROM activity_events e
                                WHERE e.created_at >= d.day AT TIME ZONE :zone AND e.created_at < (d.day + interval '1 day') AT TIME ZONE :zone) AS events,
                               (SELECT count(DISTINCT e.business_id) FROM activity_events e
                                WHERE e.created_at >= d.day AT TIME ZONE :zone AND e.created_at < (d.day + interval '1 day') AT TIME ZONE :zone) AS businesses,
                               (SELECT count(DISTINCT t.subject_id) FROM refresh_tokens t
                                WHERE t.subject_type = 'USER'
                                  AND t.created_at >= d.day AT TIME ZONE :zone AND t.created_at < (d.day + interval '1 day') AT TIME ZONE :zone) AS users
                        FROM generate_series(date_trunc('day', CAST(:now AS timestamptz) AT TIME ZONE :zone) - (:days - 1) * interval '1 day',
                                             date_trunc('day', CAST(:now AS timestamptz) AT TIME ZONE :zone),
                                             interval '1 day') AS d(day)
                        ORDER BY d.day""")
                .param("now", Timestamp.from(now)).param("zone", zone).param("days", days)
                .query((rs, i) -> new ActivityDay(rs.getObject("day", LocalDate.class), rs.getLong("events"),
                        rs.getLong("businesses"), rs.getLong("users")))
                .list();
    }

    public List<KeyCount> businessTypes() {
        return jdbc.sql("""
                        SELECT type AS key, count(*) AS count FROM businesses WHERE archived_at IS NULL
                        GROUP BY type ORDER BY count(*) DESC, type""")
                .query((rs, i) -> new KeyCount(rs.getString("key"), rs.getLong("count"))).list();
    }

    /** Open businesses with each optional module switched on, by module name. */
    public Map<String, Long> moduleCounts() {
        return jdbc.sql("""
                        SELECT m AS key, count(*) AS count FROM businesses, unnest(modules) AS m
                        WHERE archived_at IS NULL GROUP BY m""")
                .query((rs, i) -> Map.entry(rs.getString("key"), rs.getLong("count"))).list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Use of each product area since {@code since}, from the activity log (what staff recorded), check-ins and
     * customer portal sessions. Business settings and creation are left out: they are not a product area.
     */
    public List<FeatureUsage> features(Instant since) {
        return jdbc.sql("""
                        SELECT area, sum(events) AS events, count(DISTINCT business_id) AS businesses FROM (
                            SELECT CASE entity_type
                                       WHEN 'CUSTOMER' THEN 'CUSTOMERS' WHEN 'OFFERING' THEN 'CATALOG'
                                       WHEN 'MEMBERSHIP' THEN 'MEMBERSHIPS' WHEN 'INVOICE' THEN 'BILLING'
                                       WHEN 'PAYMENT' THEN 'BILLING' WHEN 'EXPENSE' THEN 'EXPENSES'
                                       WHEN 'PAYROLL' THEN 'PAYROLL' WHEN 'TASK' THEN 'TASKS'
                                       WHEN 'REQUEST' THEN 'REQUESTS' WHEN 'DAY' THEN 'DAY_BOOK'
                                       WHEN 'GROUP' THEN 'GROUPS' WHEN 'TEAM_MEMBER' THEN 'TEAM'
                                       WHEN 'INVITATION' THEN 'TEAM' WHEN 'ROLE' THEN 'TEAM'
                                   END AS area, business_id, count(*) AS events
                            FROM activity_events WHERE created_at >= :since AND entity_type <> 'BUSINESS'
                            GROUP BY 1, 2
                            UNION ALL
                            SELECT 'ATTENDANCE', business_id, count(*) FROM attendance WHERE checked_in_at >= :since GROUP BY business_id
                            UNION ALL
                            SELECT 'PORTAL', c.business_id, count(*) FROM refresh_tokens t JOIN customers c ON c.id = t.subject_id
                            WHERE t.subject_type = 'CUSTOMER' AND t.created_at >= :since GROUP BY c.business_id
                        ) usage WHERE area IS NOT NULL
                        GROUP BY area ORDER BY count(DISTINCT business_id) DESC, area""")
                .param("since", Timestamp.from(since))
                .query((rs, i) -> new FeatureUsage(rs.getString("area"), rs.getLong("events"), rs.getLong("businesses")))
                .list();
    }

    public ActivationFunnel activation(Instant now) {
        return jdbc.sql("""
                        SELECT count(*) AS created,
                               count(*) FILTER (WHERE EXISTS (SELECT 1 FROM customers c WHERE c.business_id = b.id)) AS added_customer,
                               count(*) FILTER (WHERE EXISTS (SELECT 1 FROM payments p WHERE p.business_id = b.id
                                                              AND p.kind = 'PAYMENT' AND p.voided_at IS NULL)) AS recorded_payment,
                               count(*) FILTER (WHERE (SELECT count(*) FROM team_members t
                                                       WHERE t.business_id = b.id AND t.status = 'ACTIVE') > 1) AS added_teammate,
                               count(*) FILTER (WHERE EXISTS (SELECT 1 FROM activity_events e
                                                              WHERE e.business_id = b.id AND e.created_at >= :d7)) AS active_7d
                        FROM businesses b WHERE b.archived_at IS NULL""")
                .param("d7", ago(now, 7))
                .query((rs, i) -> new ActivationFunnel(rs.getLong("created"), rs.getLong("added_customer"),
                        rs.getLong("recorded_payment"), rs.getLong("added_teammate"), rs.getLong("active_7d")))
                .single();
    }

    public enum TenantView {
        ALL(""),
        /** Someone recorded something in the last 30 days. */
        ACTIVE(" AND b.archived_at IS NULL AND la.last_activity_at >= :d30"),
        /** Open, but nothing recorded in the last 30 days. */
        DORMANT(" AND b.archived_at IS NULL AND (la.last_activity_at IS NULL OR la.last_activity_at < :d30)"),
        ARCHIVED(" AND b.archived_at IS NOT NULL");

        private final String condition;

        TenantView(String condition) {
            this.condition = condition;
        }
    }

    public enum TenantSort {
        NEWEST("b.created_at DESC, b.id DESC"),
        LAST_ACTIVE("la.last_activity_at DESC NULLS LAST, b.id DESC"),
        CUSTOMERS("customers DESC, b.id DESC"),
        NAME("lower(b.name), b.id");

        private final String orderBy;

        TenantSort(String orderBy) {
            this.orderBy = orderBy;
        }
    }

    public PageResponse<TenantRow> tenants(Instant now, TenantView view, String type, String search, TenantSort sort,
                                           int page, int size) {
        String from = """
                 FROM businesses b
                 LEFT JOIN LATERAL (SELECT u.full_name, u.email FROM team_members t
                                    JOIN roles r ON r.id = t.role_id JOIN users u ON u.id = t.user_id
                                    WHERE t.business_id = b.id AND r.system_key = 'OWNER' AND t.status = 'ACTIVE'
                                    ORDER BY t.id LIMIT 1) o ON true
                 LEFT JOIN LATERAL (SELECT max(e.created_at) AS last_activity_at FROM activity_events e
                                    WHERE e.business_id = b.id) la ON true
                 WHERE true""" + view.condition
                + (type == null ? "" : " AND b.type = :type")
                + (blank(search) ? "" : " AND (lower(b.name) LIKE :like OR lower(b.code) LIKE :like OR lower(o.email) LIKE :like)");
        long total = bindTenants(jdbc.sql("SELECT count(*) " + from), now, type, search).query(Long.class).single();
        List<TenantRow> rows = bindTenants(jdbc.sql("""
                        SELECT b.id, b.code, b.name, b.type, b.city, b.created_at, b.archived_at IS NOT NULL AS archived,
                               b.modules, o.full_name AS owner_name, o.email AS owner_email, la.last_activity_at,
                               (SELECT count(*) FROM team_members t WHERE t.business_id = b.id AND t.status = 'ACTIVE') AS team_members,
                               (SELECT count(*) FROM customers c WHERE c.business_id = b.id AND c.status = 'ACTIVE') AS customers"""
                        + " " + from + " ORDER BY " + sort.orderBy + " LIMIT :size OFFSET :offset"), now, type, search)
                .param("size", size).param("offset", page * size)
                .query((rs, i) -> new TenantRow(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("type"), rs.getString("city"), instant(rs, "created_at"), rs.getString("owner_name"),
                        rs.getString("owner_email"), rs.getLong("team_members"), rs.getLong("customers"),
                        instant(rs, "last_activity_at"), rs.getBoolean("archived"), strings(rs.getArray("modules"))))
                .list();
        return PageResponse.of(rows, page, size, total);
    }

    public enum PeopleView {
        ALL(""),
        UNVERIFIED(" AND u.email_verified_at IS NULL"),
        /** Signed up but neither created nor joined a business. */
        WITHOUT_BUSINESS(" AND NOT EXISTS (SELECT 1 FROM team_members t WHERE t.user_id = u.id AND t.status = 'ACTIVE')"),
        LOCKED(" AND u.locked_until > :now");

        private final String condition;

        PeopleView(String condition) {
            this.condition = condition;
        }
    }

    public PageResponse<PlatformUserRow> people(Instant now, PeopleView view, String search, int page, int size) {
        String from = " FROM users u WHERE true" + view.condition
                + (blank(search) ? "" : " AND (lower(u.email) LIKE :like OR lower(u.full_name) LIKE :like)");
        long total = bindPeople(jdbc.sql("SELECT count(*) " + from), now, search).query(Long.class).single();
        List<PlatformUserRow> rows = bindPeople(jdbc.sql("""
                        SELECT u.id, u.full_name, u.email, u.email_verified_at IS NOT NULL AS email_verified,
                               u.password_hash IS NOT NULL AS password_sign_in, u.google_subject IS NOT NULL AS google_sign_in,
                               u.status = 'DISABLED' AS disabled, coalesce(u.locked_until > :now, false) AS locked,
                               u.created_at, u.last_sign_in_at,
                               (SELECT count(*) FROM team_members t WHERE t.user_id = u.id AND t.status = 'ACTIVE') AS businesses,
                               (SELECT max(r.created_at) FROM refresh_tokens r
                                WHERE r.subject_type = 'USER' AND r.subject_id = u.id) AS last_active_at"""
                        + " " + from + " ORDER BY u.created_at DESC, u.id DESC LIMIT :size OFFSET :offset"), now, search)
                .param("size", size).param("offset", page * size)
                .query((rs, i) -> new PlatformUserRow(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
                        rs.getBoolean("email_verified"), rs.getBoolean("password_sign_in"), rs.getBoolean("google_sign_in"),
                        rs.getBoolean("disabled"), rs.getBoolean("locked"), rs.getLong("businesses"),
                        instant(rs, "created_at"), instant(rs, "last_sign_in_at"), instant(rs, "last_active_at")))
                .list();
        return PageResponse.of(rows, page, size, total);
    }

    public DatabaseStatus database() {
        return jdbc.sql("""
                        SELECT current_setting('server_version') AS server_version,
                               pg_database_size(current_database()) AS size_bytes,
                               (SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL
                                ORDER BY installed_rank DESC LIMIT 1) AS schema_version,
                               (SELECT count(*) FROM flyway_schema_history WHERE success) AS applied,
                               (SELECT coalesce(bool_and(success), true) FROM flyway_schema_history) AS healthy""")
                .query((rs, i) -> new DatabaseStatus(rs.getString("server_version"), rs.getLong("size_bytes"),
                        rs.getString("schema_version"), rs.getLong("applied"), rs.getBoolean("healthy")))
                .single();
    }

    public SignInStatus signIns(Instant now) {
        return jdbc.sql("""
                        SELECT (SELECT count(*) FROM sign_in_events WHERE created_at >= :d1 AND outcome = 'SUCCESS') AS succeeded,
                               (SELECT count(*) FROM sign_in_events WHERE created_at >= :d1 AND outcome <> 'SUCCESS') AS failed,
                               (SELECT count(*) FROM users WHERE locked_until > :now)
                                 + (SELECT count(*) FROM customers WHERE locked_until > :now) AS locked,
                               (SELECT count(*) FROM refresh_tokens
                                WHERE revoked_at IS NULL AND rotated_at IS NULL AND expires_at > :now) AS open_sessions""")
                .param("d1", ago(now, 1)).param("now", Timestamp.from(now))
                .query((rs, i) -> new SignInStatus(rs.getLong("succeeded"), rs.getLong("failed"), rs.getLong("locked"),
                        rs.getLong("open_sessions")))
                .single();
    }

    public AccountStatus accounts(Instant now) {
        return jdbc.sql("""
                        SELECT (SELECT count(*) FROM users WHERE email_verified_at IS NULL AND created_at < :d2) AS unverified,
                               (SELECT count(*) FROM invitations
                                WHERE accepted_at IS NULL AND revoked_at IS NULL AND expires_at > :now) AS pending,
                               (SELECT count(*) FROM invitations
                                WHERE accepted_at IS NULL AND revoked_at IS NULL AND expires_at <= :now) AS expired""")
                .param("d2", ago(now, 2)).param("now", Timestamp.from(now))
                .query((rs, i) -> new AccountStatus(rs.getLong("unverified"), rs.getLong("pending"), rs.getLong("expired")))
                .single();
    }

    private static JdbcClient.StatementSpec bindTenants(JdbcClient.StatementSpec spec, Instant now, String type, String search) {
        spec = spec.param("d30", ago(now, 30));
        if (type != null) {
            spec = spec.param("type", type);
        }
        return blank(search) ? spec : spec.param("like", SqlText.containing(search));
    }

    private static JdbcClient.StatementSpec bindPeople(JdbcClient.StatementSpec spec, Instant now, String search) {
        spec = spec.param("now", Timestamp.from(now));
        return blank(search) ? spec : spec.param("like", SqlText.containing(search));
    }

    private static Timestamp ago(Instant now, int days) {
        return Timestamp.from(now.minus(DAY.multipliedBy(days)));
    }

    private static boolean blank(String search) {
        return search == null || search.isBlank();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static List<String> strings(Array array) throws SQLException {
        return array == null ? List.of() : List.of((String[]) array.getArray());
    }
}
