package com.nitin.saas.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Shared IP-address extraction utility.
 *
 * X-Forwarded-For is trusted ONLY when TRUST_PROXY_HEADERS=true, meaning the
 * application is deployed behind a reverse proxy that sanitises the header
 * before forwarding.  Using the header unconditionally lets any client spoof
 * its IP address and corrupt audit logs or bypass rate limiting.
 */
public final class IpAddressUtil {

    private static final boolean TRUST_PROXY_HEADERS =
            Boolean.parseBoolean(
                    System.getenv().getOrDefault("TRUST_PROXY_HEADERS", "false"));

    private IpAddressUtil() {}

    public static String getClientIp(HttpServletRequest request) {
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
}