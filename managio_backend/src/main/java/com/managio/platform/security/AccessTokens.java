package com.managio.platform.security;

import com.managio.platform.config.ManagioProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Issues and validates the short-lived access tokens described in docs/ARCHITECTURE.md#security-and-tenancy. */
@Component
public class AccessTokens {

    public static final String ISSUER = "managio";
    public static final String TYPE_CLAIM = "typ";
    public static final String BUSINESS_CLAIM = "bid";
    public static final String TYPE_USER = "user";
    public static final String TYPE_CUSTOMER = "customer";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration ttl;
    private final Clock clock;

    public AccessTokens(ManagioProperties properties, Clock clock) {
        SecretKey key = new SecretKeySpec(properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator expiry = new JwtTimestampValidator(Duration.ZERO);
        expiry.setClock(clock);
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(expiry, new JwtIssuerValidator(ISSUER)));
        this.decoder = nimbusDecoder;
        this.ttl = properties.security().accessTokenTtl();
        this.clock = clock;
    }

    public IssuedToken forUser(long userId, String email) {
        return issue(JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .claim(TYPE_CLAIM, TYPE_USER)
                .claim("email", email));
    }

    public IssuedToken forCustomer(long customerId, long businessId) {
        return issue(JwtClaimsSet.builder()
                .subject(Long.toString(customerId))
                .claim(TYPE_CLAIM, TYPE_CUSTOMER)
                .claim(BUSINESS_CLAIM, businessId));
    }

    public JwtDecoder decoder() {
        return decoder;
    }

    private IssuedToken issue(JwtClaimsSet.Builder claims) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        claims.issuer(ISSUER).issuedAt(now).expiresAt(expiresAt);
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
        return new IssuedToken(token, ttl.toSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
