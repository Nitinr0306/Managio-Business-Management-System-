package com.nitin.saas.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * P8 FIX: without a request-ID header every log line from a concurrent request
 * is anonymous.  Debugging production incidents requires manually correlating
 * timestamps and user IDs across services.
 *
 * This filter:
 *   1. Reads X-Request-Id from the incoming request (so upstream proxies / API
 *      gateways can set a correlation ID that flows through).
 *   2. Falls back to a generated UUID if the header is absent.
 *   3. Stores the ID in MDC so Logback/Log4j2 patterns can include it via %X{requestId}.
 *   4. Echoes the ID back in the response header so clients can correlate their
 *      own logs with server logs.
 *   5. Always clears the MDC key after the response to prevent leakage between
 *      thread-pool-recycled threads.
 *
 * Add %X{requestId} to your Logback pattern to activate:
 *   e.g. "%d{ISO8601} [%X{requestId}] %-5level %logger{36} - %msg%n"
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Request-Id";
    private static final String MDC_KEY     = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(HEADER_NAME);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);  // critical: prevent thread-pool contamination
        }
    }
}