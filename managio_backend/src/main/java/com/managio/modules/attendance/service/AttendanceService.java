package com.managio.modules.attendance.service;

import com.managio.modules.customers.service.CustomerService;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Customer check-ins and group roll calls (ATT-1). */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final CustomerService customers;
    private final GroupService groups;
    private final JdbcClient jdbc;
    private final Clock clock;

    /** {@code covered} is false when the customer has no membership running today; the check-in still counts. */
    public record CheckIn(long id, long customerId, String customerName, int customerNumber, LocalDate date,
                          Instant checkedInAt, @Nullable String markedBy, boolean alreadyCheckedIn, boolean covered) {
    }

    @Transactional
    public CheckIn checkIn(BusinessContext ctx, long customerId) {
        customers.requireActive(ctx.businessId(), customerId);
        LocalDate today = ctx.today(clock);
        boolean inserted = jdbc.sql("""
                        INSERT INTO attendance (business_id, customer_id, attended_on, checked_in_at, marked_by_user_id)
                        VALUES (:businessId, :customerId, :day, :now, :userId)
                        ON CONFLICT (customer_id, attended_on) DO NOTHING""")
                .param("businessId", ctx.businessId()).param("customerId", customerId).param("day", today)
                .param("now", Timestamp.from(clock.instant())).param("userId", ctx.userId())
                .update() == 1;
        return find(ctx.businessId(), customerId, today, !inserted);
    }

    /** Roll call: after this, exactly the {@code present} customers of the group are marked for the date. */
    @Transactional
    public List<CheckIn> markGroup(BusinessContext ctx, long groupId, LocalDate date, Set<Long> present) {
        if (date.isAfter(ctx.today(clock))) {
            throw ApiException.invalidField("date", "Attendance can't be marked for a future date");
        }
        Set<Long> members = new HashSet<>(groups.activeMemberIds(ctx.businessId(), groupId));
        if (!members.containsAll(present)) {
            throw ApiException.invalidField("present", "Everyone marked present must be in the group");
        }
        Set<Long> absent = new HashSet<>(members);
        absent.removeAll(present);
        if (!absent.isEmpty()) {
            jdbc.sql("DELETE FROM attendance WHERE business_id = :businessId AND attended_on = :day AND customer_id IN (:ids)")
                    .param("businessId", ctx.businessId()).param("day", date).param("ids", absent).update();
        }
        for (Long customerId : present) {
            jdbc.sql("""
                            INSERT INTO attendance (business_id, customer_id, attended_on, checked_in_at, marked_by_user_id)
                            VALUES (:businessId, :customerId, :day, :now, :userId)
                            ON CONFLICT (customer_id, attended_on) DO NOTHING""")
                    .param("businessId", ctx.businessId()).param("customerId", customerId).param("day", date)
                    .param("now", Timestamp.from(clock.instant())).param("userId", ctx.userId()).update();
        }
        return forDay(ctx.businessId(), date, groupId);
    }

    @Transactional(readOnly = true)
    public List<CheckIn> forDay(long businessId, LocalDate date, Long groupId) {
        var query = jdbc.sql(SELECT + " WHERE a.business_id = :businessId AND a.attended_on = :day"
                        + (groupId == null ? "" : " AND EXISTS (SELECT 1 FROM customer_group_members gm "
                        + "WHERE gm.customer_id = a.customer_id AND gm.group_id = :groupId)")
                        + " ORDER BY a.checked_in_at DESC")
                .param("businessId", businessId).param("day", date);
        if (groupId != null) {
            query = query.param("groupId", groupId);
        }
        return query.query((rs, i) -> map(rs, false)).list();
    }

    /** Undoing a check-in is only possible on the same business day. */
    @Transactional
    public void undo(BusinessContext ctx, long attendanceId) {
        int removed = jdbc.sql("""
                        DELETE FROM attendance WHERE id = :id AND business_id = :businessId AND attended_on = :today""")
                .param("id", attendanceId).param("businessId", ctx.businessId()).param("today", ctx.today(clock))
                .update();
        if (removed == 0) {
            throw ApiException.notFound("Today's check-in");
        }
    }

    private CheckIn find(long businessId, long customerId, LocalDate day, boolean alreadyCheckedIn) {
        return jdbc.sql(SELECT + " WHERE a.business_id = :businessId AND a.customer_id = :customerId AND a.attended_on = :day")
                .param("businessId", businessId).param("customerId", customerId).param("day", day)
                .query((rs, i) -> map(rs, alreadyCheckedIn))
                .single();
    }

    private static final String SELECT = """
            SELECT a.id, a.customer_id, c.full_name, c.number, a.attended_on, a.checked_in_at, u.full_name AS marked_by,
                   EXISTS (SELECT 1 FROM memberships m WHERE m.customer_id = a.customer_id AND m.status = 'ACTIVE'
                           AND m.starts_on <= a.attended_on AND m.ends_on >= a.attended_on) AS covered
            FROM attendance a
            JOIN customers c ON c.id = a.customer_id
            LEFT JOIN users u ON u.id = a.marked_by_user_id""";

    private static CheckIn map(ResultSet rs, boolean alreadyCheckedIn) throws SQLException {
        return new CheckIn(rs.getLong("id"), rs.getLong("customer_id"), rs.getString("full_name"), rs.getInt("number"),
                rs.getObject("attended_on", LocalDate.class), rs.getTimestamp("checked_in_at").toInstant(),
                rs.getString("marked_by"), alreadyCheckedIn, rs.getBoolean("covered"));
    }
}
