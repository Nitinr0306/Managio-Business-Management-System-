package com.managio.platform.security;

import com.managio.platform.config.ManagioProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Decides the client address every later component sees through {@link HttpServletRequest#getRemoteAddr()}: rate
 * limits, sign-in records, sessions and the activity log (docs/ARCHITECTURE.md, "Client address").
 *
 * <p>{@code X-Forwarded-For} is never trusted, because anyone can send it. The web app's {@code /api} pass-through
 * instead sends the address its host reported in {@code X-Managio-Client-Ip}, together with the shared secret
 * {@code API_PROXY_SECRET} in {@code X-Managio-Proxy-Secret}. Only a request carrying the right secret has its
 * address taken from that header; every other request counts against the connecting address.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ClientAddressFilter extends OncePerRequestFilter {

    public static final String SECRET_HEADER = "X-Managio-Proxy-Secret";
    public static final String CLIENT_IP_HEADER = "X-Managio-Client-Ip";

    private final byte[] secretDigest;

    public ClientAddressFilter(ManagioProperties properties) {
        String secret = properties.security().proxySecret();
        this.secretDigest = secret == null || secret.isBlank() ? null : digest(secret);
        if (secretDigest == null) {
            log.warn("API_PROXY_SECRET is not set: client addresses come from the connection, so behind a proxy every "
                    + "user shares one rate limit. Set the same secret on the API and the web app.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new ResolvedAddressRequest(request, resolve(request)), response);
    }

    String resolve(HttpServletRequest request) {
        String connecting = request.getRemoteAddr();
        if (secretDigest == null) {
            return connecting;
        }
        String presented = request.getHeader(SECRET_HEADER);
        if (presented == null || !MessageDigest.isEqual(secretDigest, digest(presented))) {
            return connecting;
        }
        return ClientAddresses.parse(request.getHeader(CLIENT_IP_HEADER)).orElse(connecting);
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The request with the resolved address; the proxy secret is hidden from everything downstream. */
    private static final class ResolvedAddressRequest extends HttpServletRequestWrapper {

        private final String address;

        ResolvedAddressRequest(HttpServletRequest request, String address) {
            super(request);
            this.address = address;
        }

        @Override
        public String getRemoteAddr() {
            return address;
        }

        @Override
        public String getRemoteHost() {
            return address;
        }

        @Override
        public String getHeader(String name) {
            return SECRET_HEADER.equalsIgnoreCase(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return SECRET_HEADER.equalsIgnoreCase(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }
    }
}
