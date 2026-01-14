package com.nitin.saas.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter implements Filter {

    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, RequestCounter> ipMap = new ConcurrentHashMap<>();

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        if (isSensitiveEndpoint(path)) {
            String ip = req.getRemoteAddr();
            long now = Instant.now().toEpochMilli();

            RequestCounter counter = ipMap.getOrDefault(ip, new RequestCounter(0, now));

            if (now - counter.timestamp > WINDOW_MS) {
                counter = new RequestCounter(1, now);
            } else {
                counter.count++;
            }

            ipMap.put(ip, counter);

            if (counter.count > LIMIT) {
                res.setStatus(429);
                res.getWriter().write("Too many requests. Try later.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isSensitiveEndpoint(String path) {
        return path.contains("/login")
                || path.contains("/forgot-password")
                || path.contains("/refresh");
    }

    private static class RequestCounter {
        int count;
        long timestamp;

        RequestCounter(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}
