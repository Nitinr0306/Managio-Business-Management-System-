package com.managio.modules.daybook.service;

import com.managio.platform.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * DAY-3: cash payments, refunds and expenses can't be recorded or voided on a business date that has been
 * closed, because the counted cash would no longer match. Other payment methods aren't part of the cash count.
 */
@Component
@RequiredArgsConstructor
public class DayLock {

    private final JdbcClient jdbc;

    /** @param cash whether the money moved in cash */
    public void requireOpen(long businessId, LocalDate date, boolean cash) {
        if (!cash) {
            return;
        }
        boolean closed = jdbc.sql("SELECT EXISTS (SELECT 1 FROM day_closes WHERE business_id = :businessId AND business_date = :date)")
                .param("businessId", businessId).param("date", date).query(Boolean.class).single();
        if (closed) {
            throw ApiException.rule("day.closed", "The cash for " + date
                    + " is already closed, so cash can't be added or changed on that day. Record it on today's date instead.");
        }
    }
}
