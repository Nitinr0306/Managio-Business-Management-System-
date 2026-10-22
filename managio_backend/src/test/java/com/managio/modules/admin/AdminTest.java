package com.managio.modules.admin;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/** The platform console (D-028): who may use it, that its figures come from real data, and what it never shows. */
class AdminTest extends IntegrationTest {

    /** Listed in application-test.yml. */
    private static final String ADMIN_EMAIL = "platform.admin@managio.test";
    private static final List<String> ENDPOINTS = List.of("/api/v1/admin/access", "/api/v1/admin/overview",
            "/api/v1/admin/businesses", "/api/v1/admin/people", "/api/v1/admin/health");

    @BeforeEach
    void realTime() {
        // Accounts and businesses get their creation time from the database clock, so the console's windows
        // must end now rather than at the test clock's fixed start.
        clock.set(Instant.now());
    }

    @Test
    void onlyAListedVerifiedActiveAccountGetsIn() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        createBusiness(owner, "Ironwood Fitness", "FITNESS");
        Session unverifiedAdmin = signUp("Platform Admin", ADMIN_EMAIL);

        for (String endpoint : ENDPOINTS) {
            assertThat(get(null, endpoint)).hasStatus(401);
            problem(get(owner.token(), endpoint), 403, "auth.forbidden");
            // Signing up with a listed address proves nothing until the email is verified.
            problem(get(unverifiedAdmin.token(), endpoint), 403, "auth.forbidden");
        }

        ok(post(null, "/api/v1/auth/verify-email", Map.of("token", mailer.lastTokenFor(ADMIN_EMAIL))), 200);
        for (String endpoint : ENDPOINTS) {
            assertThat(get(unverifiedAdmin.token(), endpoint).getResponse().getStatus()).as(endpoint).isIn(200, 204);
        }

        jdbc.sql("UPDATE users SET status = 'DISABLED' WHERE id = :id").param("id", unverifiedAdmin.userId()).update();
        assertThat(get(unverifiedAdmin.token(), "/api/v1/admin/overview").getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    void aPortalCustomerCannotGetIn() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        String token = portalCustomer(owner, business, "Rohan Mehta", "rohan@example.com").path("accessToken").asString();

        for (String endpoint : ENDPOINTS) {
            assertThat(get(token, endpoint)).hasStatus(403);
        }
    }

    @Test
    void theOverviewCountsWhatHappened() {
        Session admin = verifiedUser("Platform Admin", ADMIN_EMAIL);
        Session ananya = verifiedUser("Ananya Rao", "ananya@example.com");
        Session vikram = verifiedUser("Vikram Shah", "vikram@example.com");
        long gym = createBusiness(ananya, "Ironwood Fitness", "FITNESS");
        createBusiness(vikram, "Glow Salon", "SALON");
        long rohan = createCustomer(ananya, gym, "Rohan Mehta", "9810011111");
        createCustomer(ananya, gym, "Meera Iyer", "9810022222");
        long plan = createPlan(ananya, gym, "Monthly", "1500", 30);
        long invoice = startMembership(ananya, gym, rohan, plan).path("invoices").get(0).path("id").asLong();
        ok(post(ananya.token(), "/api/v1/businesses/{b}/invoices/{i}/payments", Map.of("amount", "1500", "method", "UPI"),
                gym, invoice), 201);

        JsonNode overview = ok(get(admin.token(), "/api/v1/admin/overview"), 200);

        JsonNode totals = overview.path("totals");
        assertThat(totals.path("businesses").asLong()).isEqualTo(2);
        assertThat(totals.path("newBusinesses30d").asLong()).isEqualTo(2);
        assertThat(totals.path("activeBusinesses7d").asLong()).isEqualTo(2);
        assertThat(totals.path("users").asLong()).isEqualTo(3);
        assertThat(totals.path("unverifiedUsers").asLong()).isZero();
        assertThat(totals.path("activeUsers1d").asLong()).isEqualTo(3);
        assertThat(totals.path("teamSeats").asLong()).isEqualTo(2);
        assertThat(totals.path("customers").asLong()).isEqualTo(2);
        assertThat(totals.path("portalCustomers").asLong()).isZero();

        assertThat(keyCounts(overview.path("businessTypes"))).containsExactlyInAnyOrderEntriesOf(Map.of("FITNESS", 1L, "SALON", 1L));
        // Each type's default modules (docs/PRODUCT.md#business-types); every module is listed, used or not.
        assertThat(keyCounts(overview.path("modules"))).containsExactlyInAnyOrderEntriesOf(Map.of("MEMBERSHIPS", 2L,
                "PAYROLL", 2L, "ATTENDANCE", 1L, "CATALOG", 1L, "INVENTORY", 1L, "GROUPS", 0L));

        JsonNode activation = overview.path("activation");
        assertThat(activation.path("created").asLong()).isEqualTo(2);
        assertThat(activation.path("addedCustomer").asLong()).isEqualTo(1);
        assertThat(activation.path("recordedPayment").asLong()).isEqualTo(1);
        assertThat(activation.path("addedTeammate").asLong()).isZero();
        assertThat(activation.path("activeLast7Days").asLong()).isEqualTo(2);

        Map<String, JsonNode> features = new HashMap<>();
        overview.path("features").forEach(f -> features.put(f.path("area").asString(), f));
        assertThat(features.get("CUSTOMERS").path("events").asLong()).isEqualTo(2);
        assertThat(features.get("CUSTOMERS").path("businesses").asLong()).isEqualTo(1);
        assertThat(features.get("BILLING").path("businesses").asLong()).isEqualTo(1);
        assertThat(features).doesNotContainKey("BUSINESS");

        assertThat(overview.path("growth")).hasSize(12);
        assertThat(sum(overview.path("growth"), "users")).isEqualTo(3);
        assertThat(sum(overview.path("growth"), "businesses")).isEqualTo(2);
        long events = jdbc.sql("SELECT count(*) FROM activity_events").query(Long.class).single();
        assertThat(overview.path("activity")).hasSize(30);
        assertThat(sum(overview.path("activity"), "events")).isEqualTo(events);
        assertThat(overview.path("timeZone").asString()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void theDirectoriesShowAccountsAndBusinessesButNeverTheirCustomers() throws Exception {
        Session admin = verifiedUser("Platform Admin", ADMIN_EMAIL);
        Session ananya = verifiedUser("Ananya Rao", "ananya@example.com");
        Session vikram = verifiedUser("Vikram Shah", "vikram@example.com");
        long gym = createBusiness(ananya, "Ironwood Fitness", "FITNESS");
        createBusiness(vikram, "Glow Salon", "SALON");
        createCustomer(ananya, gym, "Rohan Mehta", "9810011111");
        createCustomer(ananya, gym, "Meera Iyer", "9810022222");

        MvcTestResult found = get(admin.token(), "/api/v1/admin/businesses?q=IRONWOOD");
        JsonNode page = ok(found, 200);
        assertThat(page.path("totalItems").asLong()).isEqualTo(1);
        JsonNode row = page.path("items").get(0);
        assertThat(row.path("name").asString()).isEqualTo("Ironwood Fitness");
        assertThat(row.path("type").asString()).isEqualTo("FITNESS");
        assertThat(row.path("ownerEmail").asString()).isEqualTo("ananya@example.com");
        assertThat(row.path("teamMembers").asLong()).isEqualTo(1);
        assertThat(row.path("customers").asLong()).isEqualTo(2);
        assertThat(row.path("lastActivityAt").isNull()).isFalse();
        // Tenant isolation: the console never carries a business's customers.
        String raw = found.getResponse().getContentAsString();
        assertThat(raw).doesNotContain("Rohan", "Meera", "9810011111");

        assertThat(ok(get(admin.token(), "/api/v1/admin/businesses?q=ananya@"), 200).path("totalItems").asLong()).isEqualTo(1);
        assertThat(ok(get(admin.token(), "/api/v1/admin/businesses?type=SALON"), 200).path("items").get(0).path("name").asString())
                .isEqualTo("Glow Salon");
        assertThat(ok(get(admin.token(), "/api/v1/admin/businesses?sort=CUSTOMERS"), 200).path("items").get(0).path("name").asString())
                .isEqualTo("Ironwood Fitness");
        assertThat(ok(get(admin.token(), "/api/v1/admin/businesses?view=DORMANT"), 200).path("totalItems").asLong()).isZero();
        assertThat(ok(get(admin.token(), "/api/v1/admin/businesses?view=ACTIVE"), 200).path("totalItems").asLong()).isEqualTo(2);
        // A typed % is a character, not a wildcard.
        assertThat(ok(get(admin.token(), "/api/v1/admin/businesses?q=%25"), 200).path("totalItems").asLong()).isZero();

        JsonNode person = ok(get(admin.token(), "/api/v1/admin/people?q=vikram"), 200).path("items").get(0);
        assertThat(person.path("email").asString()).isEqualTo("vikram@example.com");
        assertThat(person.path("emailVerified").asBoolean()).isTrue();
        assertThat(person.path("passwordSignIn").asBoolean()).isTrue();
        assertThat(person.path("businesses").asLong()).isEqualTo(1);
        assertThat(person.has("passwordHash")).isFalse();
        JsonNode noBusiness = ok(get(admin.token(), "/api/v1/admin/people?view=WITHOUT_BUSINESS"), 200);
        assertThat(noBusiness.path("totalItems").asLong()).isEqualTo(1);
        assertThat(noBusiness.path("items").get(0).path("email").asString()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void healthReportsTheRealState() {
        Session admin = verifiedUser("Platform Admin", ADMIN_EMAIL);
        Session ananya = verifiedUser("Ananya Rao", "ananya@example.com");
        long gym = createBusiness(ananya, "Ironwood Fitness", "FITNESS");
        ok(post(ananya.token(), "/api/v1/businesses/{b}/invitations", Map.of("email", "new.hire@example.com",
                "roleId", roleId(ananya, gym, "MANAGER")), gym), 201);
        problem(post(null, "/api/v1/auth/sign-in", Map.of("email", "ananya@example.com", "password", "wrong-password")),
                401, "auth.invalid_credentials");
        Session stale = signUp("Stale Signup", "stale@example.com");
        jdbc.sql("UPDATE users SET created_at = now() - interval '3 days' WHERE id = :id").param("id", stale.userId()).update();

        JsonNode health = ok(get(admin.token(), "/api/v1/admin/health"), 200);

        assertThat(health.path("database").path("schemaVersion").asString()).isEqualTo("3");
        assertThat(health.path("database").path("migrationsHealthy").asBoolean()).isTrue();
        assertThat(health.path("database").path("serverVersion").asString()).startsWith("17");
        assertThat(health.path("database").path("sizeBytes").asLong()).isPositive();
        assertThat(health.path("integrations").path("emailDelivery").asBoolean()).isFalse();
        assertThat(health.path("integrations").path("googleSignIn").asBoolean()).isFalse();
        assertThat(health.path("signIns").path("failed24h").asLong()).isEqualTo(1);
        assertThat(health.path("signIns").path("openSessions").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(health.path("accounts").path("pendingInvitations").asLong()).isEqualTo(1);
        assertThat(health.path("accounts").path("unverifiedOver48h").asLong()).isEqualTo(1);
        assertThat(health.path("application").path("heapMaxBytes").asLong()).isPositive();
        assertThat(health.path("application").path("javaVersion").asString()).isNotBlank();
    }

    private JsonNode portalCustomer(Session owner, long business, String name, String email) {
        long customer = ok(post(owner.token(), "/api/v1/businesses/{b}/customers", Map.of("fullName", name, "email", email),
                business), 201).path("id").asLong();
        ok(post(owner.token(), "/api/v1/businesses/{b}/customers/{c}/portal/invite", null, business, customer), 202);
        return ok(post(null, "/api/v1/portal/auth/set-password",
                Map.of("token", mailer.lastTokenFor(email), "password", "correct-horse-1")), 200);
    }

    private static Map<String, Long> keyCounts(JsonNode list) {
        Map<String, Long> counts = new HashMap<>();
        list.forEach(item -> counts.put(item.path("key").asString(), item.path("count").asLong()));
        return counts;
    }

    private static long sum(JsonNode list, String field) {
        return StreamSupport.stream(list.spliterator(), false).mapToLong(item -> item.path(field).asLong()).sum();
    }
}
