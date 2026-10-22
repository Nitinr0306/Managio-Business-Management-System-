package com.managio.platform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/** Application settings under {@code managio.*}; see application.yml for the environment variables. */
@Validated
@ConfigurationProperties("managio")
public record ManagioProperties(
        @NotBlank String appUrl,
        @Valid @NotNull Security security,
        @Valid @NotNull Google google,
        @Valid @NotNull Mail mail,
        @Valid @NotNull Admin admin) {

    public record Security(
            @NotBlank @Size(min = 32, message = "must be at least 32 characters") String jwtSecret,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            boolean secureCookies,
            List<String> corsAllowedOrigins,
            /** Multiplies every rate limit. 1 in production; local development and end-to-end runs raise it. */
            @Min(1) int rateLimitMultiplier,
            /**
             * Shared with the web app's {@code /api} pass-through, which proves with it that the client address it
             * reports is genuine (ClientAddressFilter). Empty means no proxy is trusted.
             */
            @Pattern(regexp = "|.{32,}", message = "must be empty or at least 32 characters") String proxySecret) {
    }

    /** Google sign-in is disabled when the client ID is blank. */
    public record Google(String clientId) {
        public boolean enabled() {
            return clientId != null && !clientId.isBlank();
        }
    }

    /**
     * Emails are logged instead of sent when the Resend API key is blank. Bodies (which contain single-use links)
     * are only logged when {@code logContent} is on, which is meant for local development.
     */
    public record Mail(String resendApiKey, @NotBlank String from, boolean logContent) {
        public boolean delivers() {
            return resendApiKey != null && !resendApiKey.isBlank();
        }
    }

    /**
     * Platform administrators (D-028): accounts whose email is listed here and verified. Empty means nobody, which
     * is the default. Changing the list needs a configuration change and restart, never an in-app action.
     */
    public record Admin(List<String> emails) {
        public boolean includes(String email) {
            return emails != null && email != null
                    && emails.stream().map(String::trim).anyMatch(listed -> !listed.isEmpty() && listed.equalsIgnoreCase(email));
        }
    }
}
