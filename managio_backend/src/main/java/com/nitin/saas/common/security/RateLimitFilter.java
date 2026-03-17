package com.nitin.saas.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-process fixed-window rate limiter for sensitive auth endpoints.
 *
 * Thread safety: the per-client counter uses a single synchronized method
 * {@link RequestCounter#tryAcquire} that checks the window boundary and
 * increments the count atomically, eliminating the TOCTOU race that existed
 * when window-reset and count-increment were separate non-synchronized steps.
 *
 * IP extraction: X-Forwarded-For is trusted only when TRUST_PROXY_HEADERS=true
 * (set via environment variable).  Without it, the direct remote address is used,
 * which cannot be spoofed by the client.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final boolean TRUST_PROXY_HEADERS =
            Boolean.parseBoolean(System.getenv().getOrDefault("TRUST_PROXY_HEADERS", "false"));

    private final Map<String, EndpointConfig>              endpointConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RequestCounter>> rateLimitCache  = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        endpointConfigs.put("/api/v1/auth/login",            new EndpointConfig(5,  60));
        endpointConfigs.put("/api/v1/auth/register",         new EndpointConfig(3, 300));
        endpointConfigs.put("/api/v1/auth/forgot-password",  new EndpointConfig(3, 300));
        endpointConfigs.put("/api/v1/auth/reset-password",   new EndpointConfig(3, 300));
        endpointConfigs.put("/api/v1/auth/staff/login",      new EndpointConfig(5,  60));
        endpointConfigs.put("/api/v1/members/auth/login",    new EndpointConfig(5,  60));
        endpointConfigs.put("/api/v1/members/auth/register", new EndpointConfig(3, 300));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        EndpointConfig config = endpointConfigs.get(path);

        if (config != null) {
            String clientId = getClientIdentifier(request);
            if (!isAllowed(path, clientId, config)) {
                log.warn("Rate limit exceeded for {} on path {}", clientId, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Too many requests\"," +
                                "\"message\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String endpoint, String clientId, EndpointConfig config) {
        Map<String, RequestCounter> endpointCache =
                rateLimitCache.computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>());
        RequestCounter counter =
                endpointCache.computeIfAbsent(clientId, k -> new RequestCounter());

        return counter.tryAcquire(Instant.now().getEpochSecond(),
                config.windowSeconds, config.maxRequests);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        if (TRUST_PROXY_HEADERS) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            String xri = request.getHeader("X-Real-IP");
            if (xri != null && !xri.isBlank()) {
                return xri;
            }
        }
        return request.getRemoteAddr();
    }

    // ── Inner types ────────────────────────────────────────────────────────────

    static class EndpointConfig {
        final int  maxRequests;
        final long windowSeconds;
        EndpointConfig(int maxRequests, long windowSeconds) {
            this.maxRequests   = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }

    /**
     * FIX B8: the previous implementation stored windowStart as a plain volatile
     * long and performed the check-then-act in the caller without synchronisation.
     * Two concurrent threads could both observe an expired window, both call
     * reset(), and both proceed to increment — bypassing the per-window limit.
     *
     * All state mutations are now encapsulated in the single synchronized method
     * {@link #tryAcquire}, making the check-reset-increment sequence atomic per
     * counter instance.  Because each counter is scoped to one (endpoint, clientIP)
     * pair the lock contention is negligible in practice.
     */
    static class RequestCounter {
        private long windowStart = Instant.now().getEpochSecond();
        private int  count       = 0;

        synchronized boolean tryAcquire(long now, long windowSeconds, int maxRequests) {
            if (now - windowStart > windowSeconds) {
                windowStart = now;
                count = 0;
            }
            if (count >= maxRequests) {
                return false;
            }
            count++;
            return true;
        }
    }
}