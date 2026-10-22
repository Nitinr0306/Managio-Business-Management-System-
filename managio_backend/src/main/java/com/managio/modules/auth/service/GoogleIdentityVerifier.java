package com.managio.modules.auth.service;

import com.managio.platform.config.ManagioProperties;
import com.managio.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Verifies Google ID tokens from Google Identity Services (D-010): RS256 signature against Google's published keys,
 * issuer, audience (our client ID) and expiry. Tested with tokens signed by a local key (GoogleIdentityVerifierTest).
 */
@Component
public class GoogleIdentityVerifier {

    private static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder decoder;

    @Autowired
    public GoogleIdentityVerifier(ManagioProperties properties) {
        this(properties.google().clientId(),
                properties.google().enabled() ? NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS).build() : null);
    }

    /** For tests: checks tokens against {@code decoder}'s keys instead of Google's, with the same rules. */
    GoogleIdentityVerifier(String clientId, NimbusJwtDecoder decoder) {
        if (decoder != null) {
            decoder.setJwtValidator(validator(clientId));
        }
        this.decoder = decoder;
    }

    /** Expiry (with a minute of clock skew), Google as the issuer and this deployment's client ID as the audience. */
    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Unexpected issuer", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Unexpected audience", null));
        OAuth2TokenValidator<Jwt> expiry = jwt -> jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "No expiry", null));
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), expiry, issuer, audience);
    }

    public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
    }

    public GoogleIdentity verify(String credential) {
        if (decoder == null) {
            throw ApiException.rule("google.disabled", "Google sign-in is not available. Sign in with your email and password.");
        }
        try {
            Jwt jwt = decoder.decode(credential);
            Object verified = jwt.getClaim("email_verified");
            boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(verified);
            return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"), emailVerified,
                    jwt.getClaimAsString("name"));
        } catch (JwtException e) {
            throw ApiException.unauthorized("google.invalid", "Google sign-in didn't complete. Try again.");
        }
    }
}
