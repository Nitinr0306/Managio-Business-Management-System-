package com.managio.modules.daybook.service;

import com.managio.platform.util.Money;
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
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The day's cash and its close (DAY-1, DAY-2). Everything is computed from payments and expenses. */
@Service
@RequiredArgsConstructor
public class DayBookService {

    private final JdbcClient jdbc;
    private final ActivityLog activity;
    private final Clock clock;

    public record Closed(BigDecimal countedCash, BigDecimal difference, @Nullable String notes,
                         @Nullable String closedBy, Instant closedAt) {
    }

    /**
     * @param byMethod money received per method that day, net of refunds, for information; only cash is counted
     * @param closed   present once the day has been closed
     */
    public record DayBook(LocalDate date, BigDecimal openingCash, BigDecimal cashIn, BigDecimal cashOut,
                          BigDecimal expectedCash, Map<String, BigDecimal> byMethod, @Nullable Closed closed) {
    }

    @Transactional(readOnly = true)
    public DayBook get(BusinessContext ctx, LocalDate date) {
        BigDecimal opening = jdbc.sql("""
                        SELECT counted_cash FROM day_closes WHERE business_id = :businessId AND business_date < :date
                        ORDER BY business_date DESC LIMIT 1""")
                .param("businessId", ctx.businessId()).param("date", date).query(BigDecimal.class).optional().orElse(Money.ZERO);
        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        jdbc.sql("""
                        SELECT method, sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END) AS net
                        FROM payments WHERE business_id = :businessId AND paid_on = :date AND voided_at IS NULL
                        GROUP BY method ORDER BY method""")
                .param("businessId", ctx.businessId()).param("date", date)
                .query((rs, i) -> Map.entry(rs.getString("method"), rs.getBigDecimal("net"))).list()
                .forEach(e -> byMethod.put(e.getKey(), e.getValue()));
        BigDecimal cashIn = byMethod.getOrDefault("CASH", Money.ZERO);
        BigDecimal cashOut = jdbc.sql("""
                        SELECT coalesce(sum(amount), 0) FROM expenses
                        WHERE business_id = :businessId AND spent_on = :date AND method = 'CASH' AND voided_at IS NULL""")
                .param("businessId", ctx.businessId()).param("date", date).query(BigDecimal.class).single();
        Closed closed = jdbc.sql("""
                        SELECT d.counted_cash, d.difference, d.notes, u.full_name, d.closed_at
                        FROM day_closes d LEFT JOIN users u ON u.id = d.closed_by_user_id
                        WHERE d.business_id = :businessId AND d.business_date = :date""")
                .param("businessId", ctx.businessId()).param("date", date)
                .query((rs, i) -> new Closed(rs.getBigDecimal("counted_cash"), rs.getBigDecimal("difference"),
                        rs.getString("notes"), rs.getString("full_name"), rs.getTimestamp("closed_at").toInstant()))
                .optional().orElse(null);
        return new DayBook(date, opening, cashIn, cashOut, opening.add(cashIn).subtract(cashOut), byMethod, closed);
    }

    /** DAY-2: records the counted cash and the difference. One close per date. */
    @Transactional
    public DayBook close(BusinessContext ctx, LocalDate date, BigDecimal countedCash, String notes) {
        if (date.isAfter(ctx.today(clock))) {
            throw ApiException.invalidField("date", "You can't close a day that hasn't happened yet");
        }
        BigDecimal counted = Money.of(countedCash);
        if (counted.signum() < 0) {
            throw ApiException.invalidField("countedCash", "Cash can't be negative");
        }
        DayBook book = get(ctx, date);
        if (book.closed() != null) {
            throw ApiException.conflict("day.already_closed", "This day was already closed.");
        }
        BigDecimal difference = counted.subtract(book.expectedCash());
        jdbc.sql("""
                        INSERT INTO day_closes (business_id, business_date, opening_cash, cash_in, cash_out, expected_cash,
                                                counted_cash, difference, notes, closed_by_user_id, closed_at)
                        VALUES (:businessId, :date, :opening, :cashIn, :cashOut, :expected, :counted, :difference, :notes,
                                :userId, :now)""")
                .param("businessId", ctx.businessId()).param("date", date).param("opening", book.openingCash())
                .param("cashIn", book.cashIn()).param("cashOut", book.cashOut()).param("expected", book.expectedCash())
                .param("counted", counted).param("difference", difference).param("notes", Normalize.text(notes))
                .param("userId", ctx.userId()).param("now", Timestamp.from(clock.instant())).update();
        String result = difference.signum() == 0 ? "cash matched" : (difference.signum() > 0 ? "cash over by " : "cash short by ")
                + Money.inr(difference.abs());
        activity.record(ctx, "DAY_CLOSED", "DAY", null, "Closed " + date + ": counted " + Money.inr(counted) + ", " + result);
        return get(ctx, date);
    }

    /** Recent closes, newest first, for spotting repeated differences. */
    @Transactional(readOnly = true)
    public List<DayBook> history(BusinessContext ctx, int days) {
        List<LocalDate> dates = jdbc.sql("""
                        SELECT business_date FROM day_closes WHERE business_id = :businessId
                        ORDER BY business_date DESC LIMIT :limit""")
                .param("businessId", ctx.businessId()).param("limit", Math.clamp(days, 1, 90))
                .query((rs, i) -> rs.getObject("business_date", LocalDate.class)).list();
        return dates.stream().map(d -> get(ctx, d)).toList();
    }
}
