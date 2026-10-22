package com.managio.modules.memberships.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A customer's membership status from all their non-cancelled memberships (MEM-8). The customer list
 * filters in {@code CustomerQueries} mirror these definitions in SQL.
 */
public enum MembershipState {
    ACTIVE, ENDING_SOON, UPCOMING, LAPSED, NONE;

    public static final int ENDING_SOON_DAYS = 7;

    /**
     * @param coveredToday whether some membership covers today
     * @param coveredUntil latest end date among memberships ending today or later, or null
     * @param lastEnded    latest end date among memberships that already ended, or null
     */
    public static MembershipState of(boolean coveredToday, LocalDate coveredUntil, LocalDate lastEnded, LocalDate today) {
        if (coveredToday) {
            return ChronoUnit.DAYS.between(today, coveredUntil) < ENDING_SOON_DAYS ? ENDING_SOON : ACTIVE;
        }
        if (coveredUntil != null) {
            return UPCOMING;
        }
        return lastEnded != null ? LAPSED : NONE;
    }
}
