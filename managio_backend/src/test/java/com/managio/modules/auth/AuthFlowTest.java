package com.managio.modules.auth;

import com.managio.modules.auth.service.GoogleIdentityVerifier;
import com.managio.modules.auth.service.GoogleIdentityVerifier.GoogleIdentity;
import com.managio.platform.security.AccessTokens;
import com.managio.platform.error.ApiException;
import com.managio.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Account rules IDN-1 to IDN-7 (docs/PRODUCT.md#accounts). */
class AuthFlowTest extends IntegrationTest {

    @MockitoBean
    GoogleIdentityVerifier google;

    @Autowired
    AccessTokens accessTokens;

    @Test
    void signUpSignsInAndSendsVerificationEmail() {
        MvcTestResult result = post(null, "/api/v1/auth/sign-up",
                Map.of("fullName", "Ananya Rao", "email", "Ananya@Example.com", "password", "correct-horse-1"));

        JsonNode body = ok(result, 201);
        assertThat(body.path("user").path("email").asString()).isEqualTo("ananya@example.com");
        assertThat(body.path("user").path("emailVerified").asBoolean()).isFalse();
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("mg_rt=", "HttpOnly", "SameSite=Lax");
        assertThat(mailer.lastTo("ananya@example.com").subject()).isEqualTo("Confirm your email for Managio");

        JsonNode me = ok(get(body.path("accessToken").asString(), "/api/v1/me"), 200);
        assertThat(me.path("fullName").asString()).isEqualTo("Ananya Rao");
    }

    @Test
    void signUpRejectsDuplicateEmailAndBadInput() {
        signUp("Ananya Rao", "ananya@example.com");

        problem(post(null, "/api/v1/auth/sign-up",
                Map.of("fullName", "Someone", "email", "ANANYA@example.com", "password", "correct-horse-1")),
                409, "auth.email_taken");
        MvcTestResult invalid = post(null, "/api/v1/auth/sign-up", Map.of("fullName", "", "email", "nope", "password", "short"));
        problem(invalid, 400, "validation.failed");
        assertThat(body(invalid).path("errors").propertyNames()).contains("fullName", "email", "password");
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() {
        signUp("Ananya Rao", "ananya@example.com");

        MvcTestResult wrong = post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "wrong-password"));
        MvcTestResult unknown = post(null, "/api/v1/auth/sign-in", Map.of("email", "nobody@example.com", "password", "wrong-password"));

        problem(wrong, 401, "auth.invalid_credentials");
        problem(unknown, 401, "auth.invalid_credentials");
        assertThat(body(wrong).path("detail")).isEqualTo(body(unknown).path("detail"));
    }

    @Test
    void fiveFailuresLockTheAccountAndTheLockSurvivesTheFailedRequests() {
        signUp("Ananya Rao", "ananya@example.com");
        for (int i = 0; i < 5; i++) {
            problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "wrong-password")),
                    401, "auth.invalid_credentials");
        }

        problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "correct-horse-1")),
                423, "auth.locked");
        assertThat(jdbc.sql("SELECT count(*) FROM sign_in_events WHERE outcome = 'FAILURE'").query(Long.class).single())
                .isEqualTo(6);

        clock.advance(Duration.ofMinutes(16));
        ok(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "correct-horse-1")), 200);
    }

    @Test
    void refreshRotatesTheCookieAndReuseEndsTheSession() {
        Session session = signUp("Ananya Rao", "ananya@example.com");

        MvcTestResult first = postWithCookie("/api/v1/auth/refresh", session.refresh());
        JsonNode firstBody = ok(first, 200);
        assertThat(firstBody.path("accessToken").asString()).isNotBlank();
        Cookie rotated = refreshCookie(first);
        assertThat(rotated.getValue()).isNotEqualTo(session.refresh().getValue());

        // A second tab refreshing with the old cookie straight away is tolerated.
        ok(postWithCookie("/api/v1/auth/refresh", session.refresh()), 200);

        // Replaying the old cookie later is treated as theft: the whole session family ends.
        clock.advance(Duration.ofMinutes(1));
        MvcTestResult replay = postWithCookie("/api/v1/auth/refresh", session.refresh());
        problem(replay, 401, "auth.session_expired");
        assertThat(replay.getResponse().getHeader("Set-Cookie")).contains("mg_rt=", "Max-Age=0");
        problem(postWithCookie("/api/v1/auth/refresh", rotated), 401, "auth.session_expired");
    }

    @Test
    void signOutEndsTheSession() {
        Session session = signUp("Ananya Rao", "ananya@example.com");

        MvcTestResult signOut = postWithCookie("/api/v1/auth/sign-out", session.refresh());
        assertThat(signOut).hasStatus(204);
        problem(postWithCookie("/api/v1/auth/refresh", session.refresh()), 401, "auth.session_expired");
        problem(postWithCookie("/api/v1/auth/refresh", null), 401, "auth.session_expired");
    }

    @Test
    void verificationLinkWorksOnce() {
        signUp("Ananya Rao", "ananya@example.com");
        String token = mailer.lastTokenFor("ananya@example.com");

        JsonNode verified = ok(post(null, "/api/v1/auth/verify-email", Map.of("token", token)), 200);
        assertThat(verified.path("emailVerified").asBoolean()).isTrue();
        problem(post(null, "/api/v1/auth/verify-email", Map.of("token", token)), 422, "token.invalid");
    }

    @Test
    void verificationLinkExpiresAfter48Hours() {
        signUp("Ananya Rao", "ananya@example.com");
        String token = mailer.lastTokenFor("ananya@example.com");
        clock.advance(Duration.ofHours(49));

        problem(post(null, "/api/v1/auth/verify-email", Map.of("token", token)), 422, "token.invalid");
    }

    @Test
    void passwordResetRevealsNothingAndEndsOtherSessions() {
        Session session = signUp("Ananya Rao", "ananya@example.com");

        assertThat(post(null, "/api/v1/auth/password-reset", Map.of("email", "nobody@example.com"))).hasStatus(202);
        assertThat(mailer.sentTo("nobody@example.com")).isEmpty();
        assertThat(post(null, "/api/v1/auth/password-reset", Map.of("email", "ananya@example.com"))).hasStatus(202);

        String token = mailer.lastTokenFor("ananya@example.com");
        assertThat(post(null, "/api/v1/auth/password-reset/confirm", Map.of("token", token, "password", "a-new-password-2")))
                .hasStatus(204);

        problem(postWithCookie("/api/v1/auth/refresh", session.refresh()), 401, "auth.session_expired");
        problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "correct-horse-1")),
                401, "auth.invalid_credentials");
        JsonNode signedIn = ok(post(null, "/api/v1/auth/sign-in",
                Map.of("email", "ananya@example.com", "password", "a-new-password-2")), 200);
        assertThat(signedIn.path("user").path("emailVerified").asBoolean()).isTrue();
        problem(post(null, "/api/v1/auth/password-reset/confirm", Map.of("token", token, "password", "another-one-3")),
                422, "token.invalid");
    }

    @Test
    void changingPasswordNeedsTheCurrentOne() {
        Session session = signUp("Ananya Rao", "ananya@example.com");

        MvcTestResult wrong = post(session.token(), "/api/v1/me/password",
                Map.of("currentPassword", "not-it", "newPassword", "a-new-password-2"));
        problem(wrong, 400, "validation.failed");
        assertThat(body(wrong).path("errors").path("currentPassword").asString()).isNotBlank();

        MvcTestResult changed = post(session.token(), "/api/v1/me/password",
                Map.of("currentPassword", "correct-horse-1", "newPassword", "a-new-password-2"));
        ok(changed, 200);
        problem(postWithCookie("/api/v1/auth/refresh", session.refresh()), 401, "auth.session_expired");
        ok(postWithCookie("/api/v1/auth/refresh", refreshCookie(changed)), 200);
    }

    @Test
    void googleSignInCreatesOrLinksAccounts() {
        when(google.verify("new-user")).thenReturn(new GoogleIdentity("g-1", "Priya@Example.com", true, "Priya Nair"));
        JsonNode created = ok(post(null, "/api/v1/auth/google", Map.of("credential", "new-user")), 200);
        assertThat(created.path("user").path("email").asString()).isEqualTo("priya@example.com");
        assertThat(created.path("user").path("emailVerified").asBoolean()).isTrue();
        assertThat(created.path("user").path("hasPassword").asBoolean()).isFalse();

        signUp("Ananya Rao", "ananya@example.com");
        when(google.verify("existing")).thenReturn(new GoogleIdentity("g-2", "ananya@example.com", true, "Ananya"));
        JsonNode linked = ok(post(null, "/api/v1/auth/google", Map.of("credential", "existing")), 200);
        assertThat(linked.path("user").path("googleLinked").asBoolean()).isTrue();
        assertThat(linked.path("user").path("fullName").asString()).isEqualTo("Ananya Rao");

        when(google.verify("unverified")).thenReturn(new GoogleIdentity("g-3", "x@example.com", false, "X"));
        problem(post(null, "/api/v1/auth/google", Map.of("credential", "unverified")), 422, "google.email_unverified");

        when(google.verify(anyString())).thenThrow(ApiException.unauthorized("google.invalid", "Google sign-in didn't complete."));
        problem(post(null, "/api/v1/auth/google", Map.of("credential", "forged")), 401, "google.invalid");
    }

    @Test
    void googleSignInObeysLockoutAndDisabledAccounts() {
        Session session = verifiedUser("Ananya Rao", "ananya@example.com");
        when(google.verify("ananya")).thenReturn(new GoogleIdentity("g-1", "ananya@example.com", true, "Ananya"));
        for (int i = 0; i < 5; i++) {
            problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "wrong-pass-" + i)),
                    401, "auth.invalid_credentials");
        }
        // A locked account stays locked whichever way the person signs in.
        problem(post(null, "/api/v1/auth/google", Map.of("credential", "ananya")), 423, "auth.locked");
        assertThat(jdbc.sql("SELECT google_subject FROM users WHERE id = :id").param("id", session.userId())
                .query(String.class).optional()).isEmpty();
        assertThat(jdbc.sql("SELECT count(*) FROM sign_in_events WHERE detail = 'account locked'").query(Long.class).single())
                .isEqualTo(1);

        clock.advance(Duration.ofMinutes(16));
        ok(post(null, "/api/v1/auth/google", Map.of("credential", "ananya")), 200);

        jdbc.sql("UPDATE users SET status = 'DISABLED' WHERE id = :id").param("id", session.userId()).update();
        problem(post(null, "/api/v1/auth/google", Map.of("credential", "ananya")), 403, "auth.disabled");
        problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "correct-horse-1")),
                403, "auth.disabled");
        problem(postWithCookie("/api/v1/auth/refresh", session.refresh()), 401, "auth.session_expired");
    }

    @Test
    void googleTakesOverAnAccountWhoseEmailWasNeverProven() {
        // Someone registers the address first with a password of their choosing and never verifies it.
        Session squatter = signUp("Not Priya", "priya@example.com");
        when(google.verify("priya")).thenReturn(new GoogleIdentity("g-1", "priya@example.com", true, "Priya Nair"));

        JsonNode signedIn = ok(post(null, "/api/v1/auth/google", Map.of("credential", "priya")), 200);

        assertThat(signedIn.path("user").path("emailVerified").asBoolean()).isTrue();
        assertThat(signedIn.path("user").path("hasPassword").asBoolean()).isFalse();
        problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "priya@example.com", "password", "correct-horse-1")),
                401, "auth.invalid_credentials");
        problem(postWithCookie("/api/v1/auth/refresh", squatter.refresh()), 401, "auth.session_expired");
    }

    @Test
    void googleLinksToAVerifiedAccountAndKeepsItsPassword() {
        Session session = verifiedUser("Ananya Rao", "ananya@example.com");
        when(google.verify("ananya")).thenReturn(new GoogleIdentity("g-1", "ananya@example.com", true, "Ananya"));

        JsonNode linked = ok(post(null, "/api/v1/auth/google", Map.of("credential", "ananya")), 200);

        assertThat(linked.path("user").path("googleLinked").asBoolean()).isTrue();
        assertThat(linked.path("user").path("hasPassword").asBoolean()).isTrue();
        ok(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "correct-horse-1")), 200);
        ok(postWithCookie("/api/v1/auth/refresh", session.refresh()), 200);
        // A second Google account with the same email can't take the account over.
        when(google.verify("other")).thenReturn(new GoogleIdentity("g-2", "ananya@example.com", true, "Impostor"));
        problem(post(null, "/api/v1/auth/google", Map.of("credential", "other")), 409, "google.other_account");
    }

    @Test
    void passwordResetEmailsAreLimitedPerRecipient() {
        verifiedUser("Ananya Rao", "ananya@example.com");
        mailer.clear();
        for (int i = 0; i < 3; i++) {
            String address = "198.51.100." + (i + 1);
            assertThat(mvc.post().uri("/api/v1/auth/password-reset").contentType("application/json")
                    .header("X-Managio-Proxy-Secret", "test-proxy-secret-shared-with-the-web-app")
                    .header("X-Managio-Client-Ip", address)
                    .content("{\"email\":\"ananya@example.com\"}").exchange()).hasStatus(202);
        }
        problem(mvc.post().uri("/api/v1/auth/password-reset").contentType("application/json")
                .header("X-Managio-Proxy-Secret", "test-proxy-secret-shared-with-the-web-app")
                .header("X-Managio-Client-Ip", "198.51.100.200")
                .content("{\"email\":\"ananya@example.com\"}").exchange(), 429, "rate_limited");
        assertThat(mailer.sentTo("ananya@example.com")).hasSize(3);
    }

    @Test
    void changingPasswordLimitsCurrentPasswordGuesses() {
        Session session = verifiedUser("Ananya Rao", "ananya@example.com");
        for (int i = 0; i < 5; i++) {
            problem(post(session.token(), "/api/v1/me/password",
                    Map.of("currentPassword", "guess-" + i, "newPassword", "another-horse-2")), 400, "validation.failed");
        }
        problem(post(session.token(), "/api/v1/me/password",
                Map.of("currentPassword", "correct-horse-1", "newPassword", "another-horse-2")), 429, "rate_limited");
    }

    @Test
    void apiResponsesCarrySecurityHeadersAndForeignOriginsAreRefused() {
        MvcTestResult response = get(null, "/api/v1/me");
        assertThat(response.getResponse().getHeader("Content-Security-Policy")).isEqualTo("default-src 'none'; frame-ancestors 'none'");
        assertThat(response.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getResponse().getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(response.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getResponse().getHeader("X-Robots-Tag")).isEqualTo("noindex, nofollow");

        // A cookie-authenticated call from another site is refused before it reaches the session (CSRF).
        Session session = signUp("Ananya Rao", "ananya@example.com");
        MvcTestResult crossSite = mvc.post().uri("/api/v1/auth/refresh").header("Origin", "https://evil.example")
                .cookie(session.refresh()).contentType("application/json").content("{}").exchange();
        assertThat(crossSite).hasStatus(403);
        ok(postWithCookie("/api/v1/auth/refresh", session.refresh()), 200);
        // A form post can't use the JSON endpoints at all.
        assertThat(mvc.post().uri("/api/v1/auth/sign-in").contentType("text/plain")
                .content("{\"email\":\"ananya@example.com\",\"password\":\"correct-horse-1\"}").exchange()).hasStatus(415);
    }

    @Test
    void aNewSignInEndsTheSessionTheBrowserHadAndAFailedOneDoesNot() {
        Session first = signUp("Ananya Rao", "ananya@example.com");
        String credentials = "{\"email\":\"ananya@example.com\",\"password\":\"%s\"}";

        MvcTestResult wrong = mvc.post().uri("/api/v1/auth/sign-in").cookie(first.refresh()).contentType("application/json")
                .content(credentials.formatted("wrong-password")).exchange();
        problem(wrong, 401, "auth.invalid_credentials");
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_tokens WHERE revoked_at IS NOT NULL").query(Long.class).single())
                .isZero();

        MvcTestResult again = mvc.post().uri("/api/v1/auth/sign-in").cookie(first.refresh()).contentType("application/json")
                .content(credentials.formatted("correct-horse-1")).exchange();
        ok(again, 200);
        problem(postWithCookie("/api/v1/auth/refresh", first.refresh()), 401, "auth.session_expired");
        Cookie second = refreshCookie(again);

        // The same holds for Google sign-in.
        when(google.verify("ananya")).thenReturn(new GoogleIdentity("g-1", "ananya@example.com", true, "Ananya"));
        MvcTestResult viaGoogle = mvc.post().uri("/api/v1/auth/google").cookie(second).contentType("application/json")
                .content("{\"credential\":\"ananya\"}").exchange();
        ok(viaGoogle, 200);
        problem(postWithCookie("/api/v1/auth/refresh", second), 401, "auth.session_expired");
        ok(postWithCookie("/api/v1/auth/refresh", refreshCookie(viaGoogle)), 200);
        // Exactly one session is left alive: the newest.
        assertThat(jdbc.sql("SELECT count(DISTINCT family_id) FROM refresh_tokens WHERE revoked_at IS NULL").query(Long.class).single())
                .isEqualTo(1);
    }

    @Test
    void anInvitationCanBeAcceptedWithGoogleOnlyByTheInvitedAddress() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        ok(post(owner.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "divya@example.com", "roleId", roleId(owner, business, "FRONT_DESK")), business), 201);
        String token = mailer.lastTokenFor("divya@example.com");

        when(google.verify("someone-else")).thenReturn(new GoogleIdentity("g-9", "intruder@example.com", true, "Intruder"));
        problem(post(null, "/api/v1/invitations/accept", Map.of("token", token, "googleCredential", "someone-else")),
                422, "invitation.wrong_account");
        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE email = 'intruder@example.com'").query(Long.class).single()).isZero();

        when(google.verify("divya")).thenReturn(new GoogleIdentity("g-2", "Divya@Example.com", true, "Divya Menon"));
        MvcTestResult accepted = post(null, "/api/v1/invitations/accept", Map.of("token", token, "googleCredential", "divya"));
        JsonNode body = ok(accepted, 200);
        assertThat(body.path("businessId").asLong()).isEqualTo(business);
        assertThat(body.path("session").path("user").path("googleLinked").asBoolean()).isTrue();
        assertThat(body.path("session").path("user").path("emailVerified").asBoolean()).isTrue();
        assertThat(refreshCookie(accepted).getValue()).isNotBlank();
        ok(get(body.path("session").path("accessToken").asString(), "/api/v1/businesses/{id}", business), 200);

        // The link works once.
        problem(post(null, "/api/v1/invitations/accept", Map.of("token", token, "googleCredential", "divya")),
                422, "invitation.invalid");
    }

    @Test
    void customerTokensCannotUseUserEndpoints() {
        Session session = signUp("Ananya Rao", "ananya@example.com");
        // A customer whose numeric ID equals the user's ID must not be treated as that user (v1 audit finding 1).
        String customerToken = accessTokens.forCustomer(session.userId(), 1).value();

        problem(get(customerToken, "/api/v1/me"), 403, "auth.forbidden");
        problem(get(customerToken, "/api/v1/me/businesses"), 403, "auth.forbidden");
    }

    @Test
    void protectedEndpointsNeedAValidToken() {
        problem(get(null, "/api/v1/me"), 401, "auth.required");
        problem(get("not-a-jwt", "/api/v1/me"), 401, "auth.required");

        Session session = signUp("Ananya Rao", "ananya@example.com");
        clock.advance(Duration.ofMinutes(16));
        problem(get(session.token(), "/api/v1/me"), 401, "auth.required");
    }
}
