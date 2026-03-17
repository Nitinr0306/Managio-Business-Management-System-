package com.nitin.saas.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtStartupValidator {

    @Value("${app.security.jwt.access-secret}")
    private String accessSecret;

    @Value("${app.security.jwt.refresh-secret}")
    private String refreshSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtConfiguration() {
        validateSecret(accessSecret, "Access token secret");
        validateSecret(refreshSecret, "Refresh token secret");

        if (accessSecret.equals(refreshSecret)) {
            throw new IllegalStateException(
                    "Access and refresh token secrets must be different");
        }

        log.info("JWT configuration validated successfully");
    }

    private void validateSecret(String secret, String secretName) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(secretName + " is not configured");
        }

        if (secret.length() < 32) {
            throw new IllegalStateException(
                    secretName + " must be at least 32 characters long");
        }

        if (secret.equals("changeme") || secret.equals("secret") || secret.equals("default")) {
            log.warn("WARNING: {} is using a default or weak value. Please change it in production!",
                    secretName);
        }
    }
}