package com.managio.platform.security;

import com.managio.platform.config.ManagioProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/** Refresh-token cookies (D-009): HttpOnly, SameSite=Lax, Secure outside local development. */
@Component
public class RefreshCookies {

    public static final String USER = "mg_rt";
    public static final String CUSTOMER = "mg_prt";

    private final boolean secure;
    private final Duration maxAge;

    public RefreshCookies(ManagioProperties properties) {
        this.secure = properties.security().secureCookies();
        this.maxAge = properties.security().refreshTokenTtl();
    }

    public ResponseCookie issue(String name, String token) {
        return base(name, token).maxAge(maxAge).build();
    }

    public ResponseCookie clear(String name) {
        return base(name, "").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite("Lax").path("/");
    }
}
