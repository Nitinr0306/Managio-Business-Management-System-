package com.nitin.saas.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds security-related HTTP response headers on every request.
 *
 * P7 FIX: the previous implementation only set four headers.  Missing headers:
 *   - Content-Security-Policy  : protects browser clients against XSS
 *   - Permissions-Policy       : disables unused browser APIs
 *   - Referrer-Policy          : prevents referrer leakage
 *   - Cross-Origin-*           : CORP / COEP / COOP isolation
 *   - Cache-Control            : prevents sensitive API responses being cached
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * CSP can be overridden per-environment via application properties.
     * Default is a strict policy suitable for a pure-API backend (no HTML).
     * For frontends that serve HTML/JS, override in application-prod.properties.
     */
    @Value("${app.security.csp:default-src 'none'; frame-ancestors 'none'}")
    private String contentSecurityPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── Prevent MIME-type sniffing ────────────────────────────────────────
        response.setHeader("X-Content-Type-Options", "nosniff");

        // ── Clickjacking protection ───────────────────────────────────────────
        response.setHeader("X-Frame-Options", "DENY");

        // ── Legacy XSS filter (IE/old Chrome) ────────────────────────────────
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // ── Force HTTPS for 1 year, include sub-domains ───────────────────────
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");

        // ── P7: Content Security Policy ───────────────────────────────────────
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);

        // ── P7: Disable unused browser features ───────────────────────────────
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

        // ── P7: No referrer leakage ───────────────────────────────────────────
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // ── P7: Cross-Origin isolation headers ───────────────────────────────
        response.setHeader("Cross-Origin-Opener-Policy",   "same-origin");
        response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        // ── Prevent API responses being stored in shared / proxy caches ───────
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}