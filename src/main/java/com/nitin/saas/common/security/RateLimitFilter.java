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
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, EndpointConfig> endpointConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RequestCounter>> rateLimitCache = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        endpointConfigs.put("/api/v1/auth/login", new EndpointConfig(5, 60));
        endpointConfigs.put("/api/v1/auth/register", new EndpointConfig(3, 300));
        endpointConfigs.put("/api/v1/auth/forgot-password", new EndpointConfig(3, 300));
        endpointConfigs.put("/api/v1/auth/reset-password", new EndpointConfig(3, 300));
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
                        "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String endpoint, String clientId, EndpointConfig config) {
        Map<String, RequestCounter> endpointCache = rateLimitCache.computeIfAbsent(
                endpoint, k -> new ConcurrentHashMap<>());

        RequestCounter counter = endpointCache.computeIfAbsent(
                clientId, k -> new RequestCounter());

        long now = Instant.now().getEpochSecond();

        if (now - counter.windowStart > config.windowSeconds) {
            counter.reset(now);
        }

        if (counter.count.get() >= config.maxRequests) {
            return false;
        }

        counter.count.incrementAndGet();
        return true;
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static class EndpointConfig {
        final int maxRequests;
        final long windowSeconds;

        EndpointConfig(int maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }

    static class RequestCounter {
        final AtomicInteger count = new AtomicInteger(0);
        long windowStart = Instant.now().getEpochSecond();

        void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.count.set(0);
        }
    }
}