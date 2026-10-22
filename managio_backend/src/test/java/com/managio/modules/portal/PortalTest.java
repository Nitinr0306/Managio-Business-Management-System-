package com.managio.modules.portal;

import com.managio.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The customer portal: invitations, sign-in, own-data-only access (POR-1 to POR-3) and portal logins (CUS-3). */
class PortalTest extends IntegrationTest {

    private static final String B = "/api/v1/businesses/{b}";
    private static final String AUTH = "/api/v1/portal/auth";

    private Session owner;
    private long business;
    private String code;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        code = codeOf(owner, business);
    }

    private String codeOf(Session session, long businessId) {
        for (JsonNode b : ok(get(session.token(), "/api/v1/me/businesses"), 200)) {
            if (b.path("id").asLong() == businessId) {
                return b.path("code").asString();
            }
        }
        throw new AssertionError("No business " + businessId);
    }

    private long customer(String name, String email, String phone) {
        return customerAt(owner, business, name, email, phone);
    }

    private long customerAt(Session session, long businessId, String name, String email, String phone) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", name);
        if (email != null) {
            body.put("email", email);
        }
        if (phone != null) {
            body.put("phone", phone);
        }
        return ok(post(session.token(), "/api/v1/businesses/{b}/customers", body, businessId), 201).path("id").asLong();
    }

    private MvcTestResult signIn(String businessCode, String login, String password) {
        return post(null, AUTH + "/sign-in", Map.of("businessCode", businessCode, "identifier", login, "password", password));
    }

    /** Invites the customer, follows the emailed link and returns the signed-in portal session body. */
    private JsonNode invitedAndSignedIn(long customerId, String email, String password) {
        ok(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, customerId), 202);
        return ok(post(null, AUTH + "/set-password", Map.of("token", mailer.lastTokenFor(email), "password", password)), 200);
    }

    private static Cookie portalCookie(MvcTestResult result) {
        Cookie cookie = result.getResponse().getCookie("mg_prt");
        assertThat(cookie).as("mg_prt cookie").isNotNull();
        return new Cookie("mg_prt", cookie.getValue());
    }

    @Test
    void anInvitedCustomerSetsAPasswordOnceAndSignsInWithEmailOrPhone() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", "9876543210");
        ok(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, asha), 202);

        assertThat(mailer.sentTo("asha@example.com")).hasSize(1);
        assertThat(mailer.sentTo("asha@example.com").get(0).subject()).contains("Ironwood Fitness");
        // Access is on, but there is no password yet.
        problem(signIn(code, "asha@example.com", "whatever-1"), 401, "auth.invalid_credentials");

        String token = mailer.lastTokenFor("asha@example.com");
        problem(post(null, AUTH + "/set-password", Map.of("token", token, "password", "short")), 400, "validation.failed");
        MvcTestResult set = post(null, AUTH + "/set-password", Map.of("token", token, "password", "tulip-garden-9"));
        JsonNode session = ok(set, 200);
        assertThat(session.path("customer").path("fullName").asString()).isEqualTo("Asha Verma");
        assertThat(session.path("business").path("code").asString()).isEqualTo(code);
        assertThat(portalCookie(set).getValue()).isNotBlank();
        problem(post(null, AUTH + "/set-password", Map.of("token", token, "password", "tulip-garden-9")), 422, "token.invalid");

        ok(signIn(code, "ASHA@Example.com", "tulip-garden-9"), 200);
        ok(signIn(code.toLowerCase(), "9876543210", "tulip-garden-9"), 200);
        problem(signIn(code, "asha@example.com", "tulip-garden-0"), 401, "auth.invalid_credentials");
        assertThat(ok(get(owner.token(), B + "/customers/{c}", business, asha), 200).path("portalEnabled").asBoolean()).isTrue();
    }

    @Test
    void invitingNeedsAnEmailAndTheRightPermission() {
        setUp();
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");
        long noEmail = customer("Bela", null, "9811111111");
        long asha = customer("Asha Verma", "asha@example.com", null);

        problem(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, noEmail), 422, "portal.email_required");
        problem(post(trainer.token(), B + "/customers/{c}/portal/invite", null, business, asha), 403, "auth.forbidden");
        problem(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, 999999), 404, "not_found");

        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        createBusiness(rahul, "Corner Mart", "RETAIL");
        problem(post(rahul.token(), B + "/customers/{c}/portal/invite", null, business, asha), 404, "not_found");
        assertThat(mailer.sentTo("asha@example.com")).isEmpty();
    }

    @Test
    void aCustomerSeesOnlyTheirOwnRecords() {
        setUp();
        long plan = createPlan(owner, business, "Monthly", "1500", 30);
        long asha = customer("Asha Verma", "asha@example.com", null);
        long bela = customer("Bela Rao", "bela@example.com", null);
        JsonNode ashaMembership = startMembership(owner, business, asha, plan);
        startMembership(owner, business, bela, plan);
        long ashaInvoice = ashaMembership.path("invoices").get(0).path("id").asLong();
        long payment = ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "500", "method", "UPI"), business, ashaInvoice), 201)
                .path("id").asLong();
        // Void records are not shown to the customer.
        long extra = ok(post(owner.token(), B + "/invoices", Map.of("customerId", asha,
                "lines", List.of(Map.of("description", "Locker", "unitPrice", "200"))), business), 201).path("id").asLong();
        ok(post(owner.token(), B + "/invoices/{i}/void", Map.of("reason", "Mistake"), business, extra), 200);
        long second = ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "50", "method", "CASH"), business, ashaInvoice), 201)
                .path("id").asLong();
        ok(post(owner.token(), B + "/payments/{p}/void", Map.of("reason", "Wrong amount"), business, second), 200);

        String token = invitedAndSignedIn(asha, "asha@example.com", "tulip-garden-9").path("accessToken").asString();

        JsonNode me = ok(get(token, "/api/v1/portal/me"), 200);
        assertThat(me.path("customer").path("fullName").asString()).isEqualTo("Asha Verma");
        assertThat(me.path("business").path("name").asString()).isEqualTo("Ironwood Fitness");
        assertThat(me.path("membershipState").asString()).isEqualTo("ACTIVE");
        assertThat(me.path("balanceDue").decimalValue()).isEqualByComparingTo("1000");

        JsonNode memberships = ok(get(token, "/api/v1/portal/memberships"), 200);
        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).path("planName").asString()).isEqualTo("Monthly");
        assertThat(memberships.get(0).path("state").asString()).isEqualTo("ACTIVE");

        JsonNode invoices = ok(get(token, "/api/v1/portal/invoices"), 200);
        assertThat(invoices.path("totalItems").asInt()).isEqualTo(1);
        assertThat(invoices.path("items").get(0).path("id").asLong()).isEqualTo(ashaInvoice);
        assertThat(invoices.path("items").get(0).path("balance").decimalValue()).isEqualByComparingTo("1000");

        JsonNode payments = ok(get(token, "/api/v1/portal/payments"), 200);
        assertThat(payments.path("totalItems").asInt()).isEqualTo(1);
        assertThat(payments.path("items").get(0).path("id").asLong()).isEqualTo(payment);
        assertThat(payments.path("items").get(0).path("customer").path("name").asString()).isEqualTo("Asha Verma");
    }

    @Test
    void portalAndStaffTokensDoNotOpenEachOthersDoors() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        String customerToken = invitedAndSignedIn(asha, "asha@example.com", "tulip-garden-9").path("accessToken").asString();
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");

        assertThat(get(owner.token(), "/api/v1/portal/me").getResponse().getStatus()).isEqualTo(403);
        assertThat(get(null, "/api/v1/portal/me").getResponse().getStatus()).isEqualTo(401);
        assertThat(get(customerToken, B + "/customers", business).getResponse().getStatus()).isIn(401, 403);
        assertThat(get(customerToken, "/api/v1/businesses/{b}/customers", mart).getResponse().getStatus()).isIn(401, 403);
        assertThat(get(customerToken, "/api/v1/me/businesses").getResponse().getStatus()).isIn(401, 403);
        assertThat(get(customerToken, "/api/v1/me/portfolio").getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    void failedSignInsLockTheAccountAndNothingRevealsWhichPartWasWrong() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        invitedAndSignedIn(asha, "asha@example.com", "tulip-garden-9");

        // Unknown business, unknown login and wrong password all look the same.
        MvcTestResult wrongBusiness = signIn("NOSUCH", "asha@example.com", "tulip-garden-9");
        MvcTestResult wrongLogin = signIn(code, "nobody@example.com", "tulip-garden-9");
        MvcTestResult wrongPassword = signIn(code, "asha@example.com", "nope-nope-1");
        for (MvcTestResult result : List.of(wrongBusiness, wrongLogin, wrongPassword)) {
            problem(result, 401, "auth.invalid_credentials");
        }
        assertThat(body(wrongBusiness).path("detail").asString()).isEqualTo(body(wrongPassword).path("detail").asString());

        for (int i = 0; i < 4; i++) {
            problem(signIn(code, "asha@example.com", "nope-nope-" + i), 401, "auth.invalid_credentials");
        }
        problem(signIn(code, "asha@example.com", "tulip-garden-9"), 423, "auth.locked");

        clock.advance(Duration.ofMinutes(16));
        ok(signIn(code, "asha@example.com", "tulip-garden-9"), 200);
    }

    @Test
    void passwordResetEmailsOnlyRealAccountsAndEndsOldSessions() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        MvcTestResult first = post(null, AUTH + "/set-password", Map.of("token", inviteToken(asha), "password", "tulip-garden-9"));
        ok(first, 200);
        Cookie oldSession = portalCookie(first);

        assertThat(post(null, AUTH + "/password-reset", Map.of("businessCode", code, "identifier", "asha@example.com")))
                .hasStatus(202);
        assertThat(post(null, AUTH + "/password-reset", Map.of("businessCode", code, "identifier", "nobody@example.com")))
                .hasStatus(202);
        assertThat(post(null, AUTH + "/password-reset", Map.of("businessCode", "NOSUCH", "identifier", "asha@example.com")))
                .hasStatus(202);
        assertThat(mailer.sentTo("nobody@example.com")).isEmpty();
        assertThat(mailer.sentTo("asha@example.com")).hasSize(2);

        ok(post(null, AUTH + "/set-password", Map.of("token", mailer.lastTokenFor("asha@example.com"), "password", "new-secret-77")), 200);
        problem(signIn(code, "asha@example.com", "tulip-garden-9"), 401, "auth.invalid_credentials");
        ok(signIn(code, "asha@example.com", "new-secret-77"), 200);
        problem(postWithCookie(AUTH + "/refresh", oldSession), 401, "auth.session_expired");
    }

    @Test
    void aNewPortalSignInEndsTheSessionTheBrowserHad() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        MvcTestResult set = post(null, AUTH + "/set-password", Map.of("token", inviteToken(asha), "password", "tulip-garden-9"));
        ok(set, 200);
        Cookie first = portalCookie(set);

        MvcTestResult again = mvc.post().uri(AUTH + "/sign-in").cookie(first).contentType("application/json")
                .content(json.writeValueAsString(Map.of("businessCode", code, "identifier", "asha@example.com",
                        "password", "tulip-garden-9"))).exchange();
        ok(again, 200);

        problem(postWithCookie(AUTH + "/refresh", first), 401, "auth.session_expired");
        ok(postWithCookie(AUTH + "/refresh", portalCookie(again)), 200);
    }

    @Test
    void sessionsRefreshRotateAndEndOnSignOutWithoutMixingWithStaffCookies() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        MvcTestResult set = post(null, AUTH + "/set-password", Map.of("token", inviteToken(asha), "password", "tulip-garden-9"));
        ok(set, 200);
        Cookie cookie = portalCookie(set);

        MvcTestResult refreshed = postWithCookie(AUTH + "/refresh", cookie);
        assertThat(ok(refreshed, 200).path("accessToken").asString()).isNotBlank();
        Cookie rotated = portalCookie(refreshed);
        assertThat(rotated.getValue()).isNotEqualTo(cookie.getValue());

        // A staff refresh cookie is no use here, and the portal cookie is no use for staff.
        problem(postWithCookie(AUTH + "/refresh", new Cookie("mg_prt", owner.refresh().getValue())), 401, "auth.session_expired");
        problem(postWithCookie("/api/v1/auth/refresh", new Cookie("mg_rt", rotated.getValue())), 401, "auth.session_expired");

        assertThat(postWithCookie(AUTH + "/sign-out", rotated)).hasStatus(204);
        problem(postWithCookie(AUTH + "/refresh", rotated), 401, "auth.session_expired");
    }

    @Test
    void selfSignUpIsOffUntilTheBusinessAllowsItAndNeverAttachesToAnExistingCustomer() {
        setUp();
        long existing = customer("Chitra Nair", null, "9822222222");
        Map<String, Object> signUp = new HashMap<>(Map.of("businessCode", code, "fullName", "Dev Patel",
                "email", "dev@example.com", "password", "mango-lassi-5"));

        problem(post(null, AUTH + "/sign-up", signUp), 403, "portal.signup_closed");
        assertThat(ok(get(null, AUTH + "/business/{code}", code), 200).path("signUpOpen").asBoolean()).isFalse();

        ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("MEMBERSHIPS"), "timezone", "Asia/Kolkata",
                "invoicePrefix", "INV", "receiptPrefix", "RCT", "allowCustomerSignup", true), business), 200);
        assertThat(ok(get(null, AUTH + "/business/{code}", code), 200).path("signUpOpen").asBoolean()).isTrue();

        MvcTestResult created = post(null, AUTH + "/sign-up", signUp);
        JsonNode session = ok(created, 201);
        assertThat(session.path("customer").path("fullName").asString()).isEqualTo("Dev Patel");
        assertThat(portalCookie(created).getValue()).isNotBlank();
        ok(get(session.path("accessToken").asString(), "/api/v1/portal/me"), 200);
        ok(signIn(code, "dev@example.com", "mango-lassi-5"), 200);

        rateLimiter.reset(); // sign-ups are limited to 5 per 10 minutes per address
        problem(post(null, AUTH + "/sign-up", signUp), 409, "customer.portal_login_taken");
        problem(post(null, AUTH + "/sign-up", Map.of("businessCode", code, "fullName", "No Contact", "password", "mango-lassi-5")),
                400, "validation.failed");
        problem(post(null, AUTH + "/sign-up", Map.of("businessCode", "NOSUCH", "fullName", "Dev", "email", "x@example.com",
                "password", "mango-lassi-5")), 404, "not_found");

        // Signing up with the phone of someone staff already added makes a separate record.
        JsonNode second = ok(post(null, AUTH + "/sign-up", Map.of("businessCode", code, "fullName", "Chitra N", "phone", "9822222222",
                "password", "mango-lassi-5")), 201);
        assertThat(second.path("customer").path("id").asLong()).isNotEqualTo(existing);
        assertThat(ok(get(owner.token(), B + "/customers", business), 200).path("totalItems").asInt()).isEqualTo(3);
        assertThat(ok(get(second.path("accessToken").asString(), "/api/v1/portal/memberships"), 200)).isEmpty();
    }

    @Test
    void aPortalLoginBelongsToOneCustomerPerBusiness() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", "9876543210");
        invitedAndSignedIn(asha, "asha@example.com", "tulip-garden-9");
        long twin = customer("Asha V", "ASHA@example.com", null);
        long samePhone = ok(post(owner.token(), B + "/customers", Map.of("fullName", "Asha Sr", "email", "asha.sr@example.com",
                "phone", "9876543210", "confirmDuplicate", true), business), 201).path("id").asLong();

        problem(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, twin), 409, "customer.portal_login_taken");
        problem(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, samePhone), 409, "customer.portal_login_taken");
        assertThat(ok(get(owner.token(), B + "/customers/{c}", business, twin), 200).path("portalEnabled").asBoolean()).isFalse();

        // The same email in another business is a different login.
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");
        long martAsha = customerAt(rahul, mart, "Asha Verma", "asha@example.com", null);
        ok(post(rahul.token(), "/api/v1/businesses/{b}/customers/{c}/portal/invite", null, mart, martAsha), 202);
        ok(post(null, AUTH + "/set-password", Map.of("token", mailer.lastTokenFor("asha@example.com"), "password", "other-pass-12")), 200);
        ok(signIn(codeOf(rahul, mart), "asha@example.com", "other-pass-12"), 200);
        problem(signIn(code, "asha@example.com", "other-pass-12"), 401, "auth.invalid_credentials");

        // A portal customer keeps something to sign in with.
        assertThat(patch(owner.token(), B + "/customers/{c}", Map.of("fullName", "Asha Verma"), business, asha).getResponse().getStatus())
                .isEqualTo(400);
    }

    @Test
    void turningAccessOffOrArchivingEndsItAtOnce() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        long bela = customer("Bela Rao", "bela@example.com", null);
        JsonNode ashaSession = invitedAndSignedIn(asha, "asha@example.com", "tulip-garden-9");
        JsonNode belaSession = invitedAndSignedIn(bela, "bela@example.com", "tulip-garden-8");
        ok(get(ashaSession.path("accessToken").asString(), "/api/v1/portal/me"), 200);

        ok(post(owner.token(), B + "/customers/{c}/portal/disable", null, business, asha), 200);
        problem(get(ashaSession.path("accessToken").asString(), "/api/v1/portal/me"), 401, "auth.session_expired");
        problem(signIn(code, "asha@example.com", "tulip-garden-9"), 401, "auth.invalid_credentials");

        ok(post(owner.token(), B + "/customers/{c}/archive", null, business, bela), 200);
        problem(get(belaSession.path("accessToken").asString(), "/api/v1/portal/me"), 401, "auth.session_expired");
        problem(signIn(code, "bela@example.com", "tulip-garden-8"), 401, "auth.invalid_credentials");
    }

    @Test
    void aCustomerChangesTheirPasswordWithTheCurrentOne() {
        setUp();
        long asha = customer("Asha Verma", "asha@example.com", null);
        MvcTestResult set = post(null, AUTH + "/set-password", Map.of("token", inviteToken(asha), "password", "tulip-garden-9"));
        String token = ok(set, 200).path("accessToken").asString();
        Cookie old = portalCookie(set);

        problem(post(token, "/api/v1/portal/me/password", Map.of("currentPassword", "wrong-wrong-1", "newPassword", "fresh-start-42")),
                400, "validation.failed");
        ok(post(token, "/api/v1/portal/me/password", Map.of("currentPassword", "tulip-garden-9", "newPassword", "fresh-start-42")), 200);
        problem(signIn(code, "asha@example.com", "tulip-garden-9"), 401, "auth.invalid_credentials");
        ok(signIn(code, "asha@example.com", "fresh-start-42"), 200);
        problem(postWithCookie(AUTH + "/refresh", old), 401, "auth.session_expired");
    }

    private String inviteToken(long customerId) {
        ok(post(owner.token(), B + "/customers/{c}/portal/invite", null, business, customerId), 202);
        return mailer.lastTokenFor(customerEmail(customerId));
    }

    private String customerEmail(long customerId) {
        return ok(get(owner.token(), B + "/customers/{c}", business, customerId), 200).path("email").asString();
    }
}
