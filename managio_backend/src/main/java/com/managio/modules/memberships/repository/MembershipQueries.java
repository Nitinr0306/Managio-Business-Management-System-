package com.managio.modules.memberships.repository;

import com.managio.modules.memberships.domain.Membership;
import com.managio.modules.memberships.domain.MembershipState;
import com.managio.modules.memberships.domain.Membership.State;
import com.managio.platform.web.Nullable;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Business-wide membership lists, mainly the renewal worklists ("ending soon" and "lapsed, not renewed"). */
@Component
@RequiredArgsConstructor
public class MembershipQueries {

    /** Which memberships a list shows (MEM-2). */
    public enum View {
        ALL("TRUE"),
        ACTIVE("m.status = 'ACTIVE' AND m.starts_on <= :today AND m.ends_on >= :today"),
        ENDING_SOON("m.status = 'ACTIVE' AND m.starts_on <= :today AND m.ends_on >= :today AND m.ends_on < :today + 7"),
        UPCOMING("m.status = 'ACTIVE' AND m.starts_on > :today"),
        EXPIRED("m.status = 'ACTIVE' AND m.ends_on < :today"),
        CANCELLED("m.status = 'CANCELLED'");

        private final String condition;

        View(String condition) {
            this.condition = condition;
        }
    }

    public record MembershipRow(long id, long customerId, int customerNumber, String customerName,
                                @Nullable String customerPhone, String planName, LocalDate startsOn, LocalDate endsOn,
                                BigDecimal price, State state, long daysLeft) {
    }

    private final JdbcClient jdbc;

    /**
     * @param needsRenewal only memberships of active customers that nobody has renewed yet, so the list is
     *                     exactly who to chase
     */
    @Transactional(readOnly = true)
    public PageResponse<MembershipRow> list(long businessId, LocalDate today, View view, boolean needsRenewal,
                                            int page, int size) {
        String where = " WHERE m.business_id = :businessId AND " + view.condition
                + (needsRenewal ? " AND c.status = 'ACTIVE' AND NOT EXISTS (SELECT 1 FROM memberships r "
                + "WHERE r.renewed_from_id = m.id AND r.status = 'ACTIVE')" : "");
        String from = " FROM memberships m JOIN customers c ON c.id = m.customer_id JOIN offerings o ON o.id = m.offering_id";
        long total = jdbc.sql("SELECT count(*)" + from + where).param("businessId", businessId).param("today", today)
                .query(Long.class).single();
        List<MembershipRow> rows = jdbc.sql("""
                        SELECT m.id, m.customer_id, c.number, c.full_name, c.phone, o.name AS plan_name, m.starts_on,
                               m.ends_on, m.price, m.status""" + from + where
                        + " ORDER BY m.ends_on ASC, m.id ASC LIMIT :limit OFFSET :offset")
                .param("businessId", businessId).param("today", today).param("limit", size).param("offset", page * size)
                .query((rs, i) -> {
                    LocalDate startsOn = rs.getObject("starts_on", LocalDate.class);
                    LocalDate endsOn = rs.getObject("ends_on", LocalDate.class);
                    Membership.Status status = Membership.Status.valueOf(rs.getString("status"));
                    return new MembershipRow(rs.getLong("id"), rs.getLong("customer_id"), rs.getInt("number"),
                            rs.getString("full_name"), rs.getString("phone"), rs.getString("plan_name"), startsOn, endsOn,
                            rs.getBigDecimal("price"), stateOf(status, startsOn, endsOn, today),
                            ChronoUnit.DAYS.between(today, endsOn));
                }).list();
        return PageResponse.of(rows, page, size, total);
    }

    public static State stateOf(Membership.Status status, LocalDate startsOn, LocalDate endsOn, LocalDate today) {
        if (status == Membership.Status.CANCELLED) {
            return State.CANCELLED;
        }
        if (startsOn.isAfter(today)) {
            return State.UPCOMING;
        }
        if (endsOn.isBefore(today)) {
            return State.EXPIRED;
        }
        return ChronoUnit.DAYS.between(today, endsOn) < MembershipState.ENDING_SOON_DAYS
                ? State.ENDING_SOON : State.ACTIVE;
    }
}
