package com.managio.support;

import com.managio.platform.security.RateLimiter;
import com.managio.support.TestInfrastructure.MutableClock;
import com.managio.support.TestInfrastructure.RecordingMailer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for tests that run the whole application against PostgreSQL. Every test starts with an empty database
 * (except the schema), a fresh clock and no recorded email.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInfrastructure.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvcTester mvc;
    @Autowired
    protected JsonMapper json;
    @Autowired
    protected JdbcClient jdbc;
    @Autowired
    protected RecordingMailer mailer;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected RateLimiter rateLimiter;

    @BeforeEach
    void resetState() {
        List<String> tables = jdbc.sql("""
                        SELECT tablename FROM pg_tables
                        WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'""")
                .query(String.class).list();
        jdbc.sql("TRUNCATE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE").update();
        mailer.clear();
        clock.reset();
        rateLimiter.reset();
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    protected MvcTestResult get(String token, String uri, Object... vars) {
        return mvc.get().uri(uri, vars).headers(h -> bearer(h, token)).exchange();
    }

    protected MvcTestResult post(String token, String uri, Object body, Object... vars) {
        return mvc.post().uri(uri, vars).headers(h -> bearer(h, token)).contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "{}" : json.writeValueAsString(body)).exchange();
    }

    protected MvcTestResult patch(String token, String uri, Object body, Object... vars) {
        return mvc.patch().uri(uri, vars).headers(h -> bearer(h, token)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)).exchange();
    }

    protected MvcTestResult put(String token, String uri, Object body, Object... vars) {
        return mvc.put().uri(uri, vars).headers(h -> bearer(h, token)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)).exchange();
    }

    protected MvcTestResult delete(String token, String uri, Object... vars) {
        return mvc.delete().uri(uri, vars).headers(h -> bearer(h, token)).exchange();
    }

    protected MvcTestResult postWithCookie(String uri, Cookie cookie) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content("{}");
        if (cookie != null) {
            request = request.cookie(cookie);
        }
        return request.exchange();
    }

    protected JsonNode body(MvcTestResult result) {
        try {
            return json.readTree(result.getResponse().getContentAsString());
        } catch (Exception e) {
            throw new AssertionError("Response is not JSON", e);
        }
    }

    protected JsonNode ok(MvcTestResult result, int status) {
        assertThat(result).as(() -> responseText(result)).hasStatus(status);
        String content = responseText(result);
        return content.isBlank() ? json.createObjectNode() : body(result);
    }

    /** Asserts an error response with the given status and problem code. */
    protected void problem(MvcTestResult result, int status, String code) {
        assertThat(result).as(() -> responseText(result)).hasStatus(status);
        assertThat(body(result).path("code").asString()).as(() -> responseText(result)).isEqualTo(code);
    }

    protected static Cookie refreshCookie(MvcTestResult result) {
        MockHttpServletResponse response = result.getResponse();
        Cookie cookie = response.getCookie("mg_rt");
        if (cookie == null) {
            throw new AssertionError("No mg_rt cookie in response");
        }
        return new Cookie("mg_rt", cookie.getValue());
    }

    private static String responseText(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (Exception e) {
            return "<unreadable>";
        }
    }

    private static void bearer(HttpHeaders headers, String token) {
        if (token != null) {
            headers.setBearerAuth(token);
        }
    }

    // ── Domain helpers ───────────────────────────────────────────────────────

    /** A signed-in user: their access token, refresh cookie and IDs. */
    protected record Session(String token, Cookie refresh, long userId, String email) {
    }

    protected Session signUp(String name, String email) {
        MvcTestResult result = post(null, "/api/v1/auth/sign-up",
                Map.of("fullName", name, "email", email, "password", "correct-horse-1"));
        JsonNode body = ok(result, 201);
        return new Session(body.path("accessToken").asString(), refreshCookie(result), body.path("user").path("id").asLong(),
                email);
    }

    protected Session verifiedUser(String name, String email) {
        Session session = signUp(name, email);
        ok(post(null, "/api/v1/auth/verify-email", Map.of("token", mailer.lastTokenFor(email))), 200);
        return session;
    }

    protected long createBusiness(Session owner, String name, String type) {
        return ok(post(owner.token(), "/api/v1/businesses", Map.of("name", name, "type", type)), 201).path("id").asLong();
    }

    protected long roleId(Session session, long businessId, String systemKeyOrName) {
        for (JsonNode role : ok(get(session.token(), "/api/v1/businesses/{id}/roles", businessId), 200)) {
            if (systemKeyOrName.equals(role.path("systemKey").asString()) || systemKeyOrName.equals(role.path("name").asString())) {
                return role.path("id").asLong();
            }
        }
        throw new AssertionError("No role " + systemKeyOrName);
    }

    /** Signs the same user in again, e.g. after the test clock moved past the token and session lifetimes. */
    protected Session reauth(Session session) {
        MvcTestResult result = post(null, "/api/v1/auth/sign-in", Map.of("email", session.email(), "password", "correct-horse-1"));
        JsonNode body = ok(result, 200);
        return new Session(body.path("accessToken").asString(), refreshCookie(result), session.userId(), session.email());
    }

    /** Switches on every optional module so tests can use groups, attendance, catalog and stock. */
    protected void enableAllModules(Session owner, long businessId) {
        ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of(
                "modules", List.of("MEMBERSHIPS", "CATALOG", "INVENTORY", "GROUPS", "ATTENDANCE", "PAYROLL"),
                "timezone", "Asia/Kolkata", "invoicePrefix", "INV", "receiptPrefix", "RCT"), businessId), 200);
    }

    protected long createCustomer(Session session, long businessId, String name, String phone) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("fullName", name);
        if (phone != null) {
            body.put("phone", phone);
        }
        return ok(post(session.token(), "/api/v1/businesses/{id}/customers", body, businessId), 201).path("id").asLong();
    }

    /** A plan lasting {@code count} {@code unit}s (DAYS or MONTHS), paid in {@code installments} invoices. */
    protected long createPlan(Session session, long businessId, String name, String price, int count, String unit,
                              int installments) {
        return ok(post(session.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PLAN", "name", name,
                "price", price, "durationValue", count, "durationUnit", unit, "installments", installments), businessId), 201)
                .path("id").asLong();
    }

    protected long createPlan(Session session, long businessId, String name, String price, int days) {
        return createPlan(session, businessId, name, price, days, "DAYS", 1);
    }

    /** Starts a membership for today and returns the response body. */
    protected JsonNode startMembership(Session session, long businessId, long customerId, long planId) {
        return ok(post(session.token(), "/api/v1/businesses/{id}/memberships",
                Map.of("customerId", customerId, "offeringId", planId), businessId), 201);
    }

    /** Invites {@code email} with the given role and accepts as a brand-new account. */
    protected Session joinAs(Session inviter, long businessId, long roleId, String name, String email) {
        ok(post(inviter.token(), "/api/v1/businesses/{id}/invitations", Map.of("email", email, "roleId", roleId), businessId), 201);
        MvcTestResult accepted = post(null, "/api/v1/invitations/accept",
                Map.of("token", mailer.lastTokenFor(email), "fullName", name, "password", "correct-horse-1"));
        JsonNode body = ok(accepted, 200).path("session");
        return new Session(body.path("accessToken").asString(), refreshCookie(accepted),
                body.path("user").path("id").asLong(), email);
    }
}
