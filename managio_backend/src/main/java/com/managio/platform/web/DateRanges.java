package com.managio.platform.web;

import com.managio.platform.error.ApiException;

import java.time.LocalDate;

/** Checks on optional {@code from}/{@code to} filters, so a reversed range is an error instead of an empty list. */
public final class DateRanges {

    private DateRanges() {
    }

    public static void requireOrdered(@Nullable LocalDate from, @Nullable LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw ApiException.invalidField("to", "The end date can't be before the start date");
        }
    }
}
