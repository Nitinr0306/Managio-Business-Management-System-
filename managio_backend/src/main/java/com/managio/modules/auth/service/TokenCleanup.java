package com.managio.modules.auth.service;

import com.managio.platform.persistence.IdempotentRequests;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Deletes refresh tokens and one-time links a week after they expired, and idempotency keys a week after they were
 * used, so the tables don't grow without end.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanup {

    static final Duration KEEP_EXPIRED_FOR = Duration.ofDays(7);

    private final Sessions sessions;
    private final OneTimeTokens oneTimeTokens;
    private final IdempotentRequests idempotentRequests;
    private final Clock clock;

    @Scheduled(cron = "${managio.jobs.token-cleanup-cron:0 30 3 * * *}", zone = "Asia/Kolkata")
    void scheduled() {
        log.info("Token cleanup removed {} rows", run());
    }

    /** Returns how many rows were removed. */
    public int run() {
        Instant cutoff = clock.instant().minus(KEEP_EXPIRED_FOR);
        return sessions.deleteExpiredBefore(cutoff) + oneTimeTokens.deleteExpiredBefore(cutoff)
                + idempotentRequests.deleteOlderThanAWeek();
    }
}
