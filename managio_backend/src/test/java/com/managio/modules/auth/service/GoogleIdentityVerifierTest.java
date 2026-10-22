package com.managio.modules.auth.service;

import com.managio.modules.auth.service.GoogleIdentityVerifier.GoogleIdentity;
import com.managio.platform.error.ApiException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The checks on a Google ID token, with real RS256 tokens signed by a local key standing in for Google's.
 * The sign-in rules that follow a verified token are in AuthFlowTest.
 */
class GoogleIdentityVerifierTest {

    private static final String CLIENT_ID = "1234567890-test.apps.googleusercontent.com";
    private static RSAKey googleKey;
    private static RSAKey otherKey;

    @BeforeAll
    static void keys() throws JOSEException {
        googleKey = new RSAKeyGenerator(2048).keyID("google").generate();
        otherKey = new RSAKeyGenerator(2048).keyID("other").generate();
    }

    private static GoogleIdentityVerifier verifier() throws JOSEException {
        return new GoogleIdentityVerifier(CLIENT_ID, NimbusJwtDecoder.withPublicKey(googleKey.toRSAPublicKey()).build());
    }

    private static String token(RSAKey key, Consumer<JWTClaimsSet.Builder> changes) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("google-subject-1")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofHours(1))))
                .claim("email", "priya@example.com")
                .claim("email_verified", true)
                .claim("name", "Priya Nair");
        changes.accept(claims);
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static void rejected(String credential) throws JOSEException {
        assertThatThrownBy(() -> verifier().verify(credential))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.code()).isEqualTo("google.invalid");
                });
    }

    @Test
    void aValidTokenGivesTheGoogleIdentity() throws JOSEException {
        GoogleIdentity identity = verifier().verify(token(googleKey, c -> { }));
        assertThat(identity).isEqualTo(new GoogleIdentity("google-subject-1", "priya@example.com", true, "Priya Nair"));
    }

    @Test
    void bothGoogleIssuerSpellingsAreAccepted() throws JOSEException {
        assertThat(verifier().verify(token(googleKey, c -> c.issuer("accounts.google.com"))).subject())
                .isEqualTo("google-subject-1");
    }

    @Test
    void anUnverifiedGoogleEmailIsReportedSoSignInCanRefuseIt() throws JOSEException {
        assertThat(verifier().verify(token(googleKey, c -> c.claim("email_verified", false))).emailVerified()).isFalse();
        assertThat(verifier().verify(token(googleKey, c -> c.claim("email_verified", "true"))).emailVerified()).isTrue();
    }

    @Test
    void aTokenForAnotherApplicationIsRejected() throws JOSEException {
        rejected(token(googleKey, c -> c.audience("someone-elses-client.apps.googleusercontent.com")));
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() throws JOSEException {
        rejected(token(googleKey, c -> c.issuer("https://evil.example")));
    }

    @Test
    void anExpiredTokenIsRejected() throws JOSEException {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        rejected(token(googleKey, c -> c.issueTime(Date.from(past)).expirationTime(Date.from(past.plus(Duration.ofHours(1))))));
    }

    @Test
    void aTokenWithoutExpiryIsRejected() throws JOSEException {
        rejected(token(googleKey, c -> c.expirationTime(null)));
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() throws JOSEException {
        rejected(token(otherKey, c -> { }));
    }

    @Test
    void aTamperedTokenIsRejected() throws JOSEException {
        String[] parts = token(googleKey, c -> { }).split("\\.");
        String forged = token(googleKey, c -> c.claim("email", "victim@example.com")).split("\\.")[1];
        rejected(parts[0] + "." + forged + "." + parts[2].substring(0, parts[2].length() - 4) + "AAAA");
    }

    @Test
    void unsignedAndSymmetricTokensAreRejected() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer("https://accounts.google.com").audience(CLIENT_ID)
                .subject("x").expirationTime(Date.from(Instant.now().plusSeconds(600))).build();
        rejected(new PlainJWT(claims).serialize());
        SignedJWT hmac = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        hmac.sign(new MACSigner("a-shared-secret-that-is-long-enough-for-hs256!!"));
        rejected(hmac.serialize());
    }

    @Test
    void garbageIsRejected() throws JOSEException {
        rejected("not-a-token");
    }

    @Test
    void withoutAClientIdGoogleSignInIsOff() {
        assertThatThrownBy(() -> new GoogleIdentityVerifier(CLIENT_ID, null).verify("anything"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("google.disabled"));
    }
}
