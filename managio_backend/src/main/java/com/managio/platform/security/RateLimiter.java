package com.managio.platform.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.managio.platform.config.ManagioProperties;
import com.managio.platform.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, in-memory rate limits for abuse-prone endpoints. Each policy keeps at most 100,000 counters, which
 * expire with their window, so memory stays bounded. One API instance runs (docs/ARCHITECTURE.md), so in-memory
 * state is sufficient; counters start again after a restart, while account lockout is stored in the database.
 * {@code managio.security.rate-limit-multiplier} scales every limit, so local and end-to-end runs from one
 * address are not refused; production keeps 1.
 */
@Component
public class RateLimiter {

    public enum Policy {
        /** Per client address: password and Google sign-in attempts. */
        SIGN_IN(10, Duration.ofMinutes(1)),
        /** Per client address: new accounts. */
        SIGN_UP(5, Duration.ofMinutes(10)),
        /** Per client address: requests that send an email (password reset, verification). */
        EMAIL(5, Duration.ofMinutes(15)),
        /** Per recipient: emails to one address, whichever addresses ask for them. */
        EMAIL_PER_RECIPIENT(3, Duration.ofHours(1)),
        /** Per account: current-password checks when changing a password. */
        PASSWORD_CHECK(5, Duration.ofMinutes(15)),
        /** Per client address: refresh-token rotations. */
        REFRESH(60, Duration.ofMinutes(1));

        private final int limit;
        private final Duration window;

        Policy(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }
    }

    private final Map<Policy, Cache<String, AtomicInteger>> counters = new ConcurrentHashMap<>();
    private final int multiplier;

    public RateLimiter(ManagioProperties properties) {
        this.multiplier = properties.security().rateLimitMultiplier();
    }

    /** Counts one attempt from the request's client address (IPv6 grouped by /64, see {@link ClientAddresses}). */
    public void checkClient(Policy policy, HttpServletRequest request) {
        check(policy, ClientAddresses.rateLimitKey(request.getRemoteAddr()));
    }

    /** Counts one attempt for {@code key} and rejects it with 429 once the policy's limit is exceeded. */
    public void check(Policy policy, String key) {
        Cache<String, AtomicInteger> cache = counters.computeIfAbsent(policy, p -> Caffeine.newBuilder()
                .expireAfterWrite(p.window)
                .maximumSize(100_000)
                .build());
        int attempts = cache.get(key, k -> new AtomicInteger()).incrementAndGet();
        if (attempts > policy.limit * multiplier) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many attempts. Wait a few minutes and try again.");
        }
    }

    /** For tests: forget all counters. */
    public void reset() {
        counters.values().forEach(Cache::invalidateAll);
    }
}
