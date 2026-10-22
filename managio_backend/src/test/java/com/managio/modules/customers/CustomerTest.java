package com.managio.modules.customers;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Customers, groups, attendance and CSV: CUS-1 to CUS-4, ATT-1. */
class CustomerTest extends IntegrationTest {

    private static final String CUSTOMERS = "/api/v1/businesses/{id}/customers";

    private Session owner;
    private long business;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        enableAllModules(owner, business);
    }

    @Test
    void customersAreNumberedPerBusinessAndNormalised() {
        setUp();
        JsonNode first = ok(post(owner.token(), CUSTOMERS, Map.of("fullName", "  Rohan Mehta ", "phone", "98450 12873",
                "email", "Rohan@Example.com"), business), 201);
        JsonNode second = ok(post(owner.token(), CUSTOMERS, Map.of("fullName", "Priya Nair"), business), 201);

        assertThat(first.path("number").asInt()).isEqualTo(1);
        assertThat(first.path("fullName").asString()).isEqualTo("Rohan Mehta");
        assertThat(first.path("phone").asString()).isEqualTo("9845012873");
        assertThat(first.path("email").asString()).isEqualTo("rohan@example.com");
        assertThat(first.path("membershipState").asString()).isEqualTo("NONE");
        assertThat(second.path("number").asInt()).isEqualTo(2);

        long otherBusiness = createBusiness(owner, "Prana Yoga", "FITNESS");
        assertThat(ok(post(owner.token(), CUSTOMERS, Map.of("fullName", "Sameer"), otherBusiness), 201).path("number").asInt())
                .isEqualTo(1);
    }

    @Test
    void aSharedPhoneNeedsConfirmation() {
        setUp();
        createCustomer(owner, business, "Rohan Mehta", "9845012873");

        MvcTestResult duplicate = post(owner.token(), CUSTOMERS, Map.of("fullName", "Rohan's wife", "phone", "98450-12873"), business);
        problem(duplicate, 409, "customer.duplicate_phone");
        assertThat(body(duplicate).path("detail").asString()).contains("Rohan Mehta", "#1");

        ok(post(owner.token(), CUSTOMERS, Map.of("fullName", "Rohan's wife", "phone", "98450-12873",
                "confirmDuplicate", true), business), 201);
    }

    @Test
    void validationReportsEveryField() {
        setUp();
        MvcTestResult invalid = post(owner.token(), CUSTOMERS, Map.of("fullName", " ", "email", "nope",
                "dateOfBirth", "2999-01-01"), business);

        problem(invalid, 400, "validation.failed");
        assertThat(body(invalid).path("errors").propertyNames()).contains("fullName", "email", "dateOfBirth");
    }

    @Test
    void searchFindsByNameNumberPhoneAndEmail() {
        setUp();
        long rohan = createCustomer(owner, business, "Rohan Mehta", "9845012873");
        createCustomer(owner, business, "Priya Nair", "9900145211");
        createCustomer(owner, business, "Meera Pillai", null);

        assertThat(names(search("roh"))).containsExactly("Rohan Mehta");
        assertThat(names(search("98450"))).containsExactly("Rohan Mehta");
        assertThat(names(search("#2"))).containsExactly("Priya Nair");
        assertThat(names(search("nair"))).containsExactly("Priya Nair");
        assertThat(names(search("zzz"))).isEmpty();
        assertThat(names(search("%"))).isEmpty();
        assertThat(ok(get(owner.token(), CUSTOMERS + "/{c}", business, rohan), 200).path("fullName").asString()).isEqualTo("Rohan Mehta");
    }

    @Test
    void archivedCustomersAreHiddenAndCanBeRestored() {
        setUp();
        long rohan = createCustomer(owner, business, "Rohan Mehta", "9845012873");
        long plan = createPlan(owner, business, "Monthly", "2499", 30);

        ok(post(owner.token(), CUSTOMERS + "/{c}/archive", null, business, rohan), 200);

        assertThat(names(ok(get(owner.token(), CUSTOMERS, business), 200))).isEmpty();
        assertThat(names(ok(get(owner.token(), CUSTOMERS + "?view=ARCHIVED", business), 200))).containsExactly("Rohan Mehta");
        // CUS-2: no new memberships or invoices for archived customers.
        problem(post(owner.token(), "/api/v1/businesses/{id}/memberships", Map.of("customerId", rohan, "offeringId", plan), business),
                422, "customer.archived");
        // CUS-4 only compares active customers, so an archived customer's phone can be reused.
        createCustomer(owner, business, "New Rohan", "9845012873");

        ok(post(owner.token(), CUSTOMERS + "/{c}/restore", null, business, rohan), 200);
        assertThat(names(ok(get(owner.token(), CUSTOMERS, business), 200))).containsExactlyInAnyOrder("Rohan Mehta", "New Rohan");
    }

    @Test
    void countsMatchTheFiltersAndStatusFollowsTheMembership() {
        setUp();
        long ending = createCustomer(owner, business, "Ends soon", null);
        long active = createCustomer(owner, business, "Active", null);
        long lapsing = createCustomer(owner, business, "Lapsing", null);
        createCustomer(owner, business, "No plan", null);
        startMembership(owner, business, ending, createPlan(owner, business, "Week", "500", 5));
        startMembership(owner, business, active, createPlan(owner, business, "Quarter", "6499", 90));
        startMembership(owner, business, lapsing, createPlan(owner, business, "Three days", "300", 3));

        assertThat(status(ending)).isEqualTo("ENDING_SOON");
        assertThat(status(active)).isEqualTo("ACTIVE");
        JsonNode counts = ok(get(owner.token(), CUSTOMERS + "/counts", business), 200);
        assertThat(counts.path("all").asInt()).isEqualTo(4);
        assertThat(counts.path("endingSoon").asInt()).isEqualTo(2);
        assertThat(counts.path("noPlan").asInt()).isEqualTo(1);
        assertThat(counts.path("lapsed").asInt()).isZero();

        clock.advance(Duration.ofDays(4));
        owner = reauth(owner);
        assertThat(status(lapsing)).isEqualTo("LAPSED");
        assertThat(names(ok(get(owner.token(), CUSTOMERS + "?view=LAPSED", business), 200))).containsExactly("Lapsing");
        assertThat(names(ok(get(owner.token(), CUSTOMERS + "?view=ENDING_SOON&sort=ENDING", business), 200)))
                .containsExactly("Ends soon");
        assertThat(ok(get(owner.token(), CUSTOMERS + "/counts", business), 200).path("lapsed").asInt()).isEqualTo(1);

        clock.advance(Duration.ofDays(1));
        owner = reauth(owner);
        assertThat(ok(get(owner.token(), CUSTOMERS + "/counts", business), 200).path("lapsed").asInt()).isEqualTo(2);
    }

    @Test
    void permissionsAndTenancyApply() {
        setUp();
        long rohan = createCustomer(owner, business, "Rohan Mehta", null);
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");

        ok(get(trainer.token(), CUSTOMERS, business), 200);
        problem(post(trainer.token(), CUSTOMERS, Map.of("fullName", "X"), business), 403, "auth.forbidden");
        problem(post(trainer.token(), CUSTOMERS + "/{c}/archive", null, business, rohan), 403, "auth.forbidden");
        problem(get(trainer.token(), CUSTOMERS + "/export", business), 403, "auth.forbidden");
        // A customer ID from another business is just "not found".
        problem(get(rahul.token(), CUSTOMERS + "/{c}", mart, rohan), 404, "not_found");
        problem(patch(rahul.token(), CUSTOMERS + "/{c}", Map.of("fullName", "Hijacked"), mart, rohan), 404, "not_found");
        assertThat(names(ok(get(rahul.token(), CUSTOMERS, mart), 200))).isEmpty();
    }

    @Test
    void groupsHoldCustomersWithinOneBusiness() {
        setUp();
        long a = createCustomer(owner, business, "Aarav", null);
        long b = createCustomer(owner, business, "Bina", null);
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long otherBusiness = createBusiness(rahul, "Corner Mart", "RETAIL");
        enableAllModules(rahul, otherBusiness);
        long outsider = createCustomer(rahul, otherBusiness, "Outsider", null);

        long group = ok(post(owner.token(), "/api/v1/businesses/{id}/groups", Map.of("name", "Morning batch"), business), 201)
                .path("id").asLong();
        problem(post(owner.token(), "/api/v1/businesses/{id}/groups", Map.of("name", "MORNING BATCH"), business), 409, "group.name_taken");
        JsonNode filled = ok(post(owner.token(), "/api/v1/businesses/{b}/groups/{g}/members",
                Map.of("customerIds", List.of(a, b)), business, group), 200);
        assertThat(filled.path("memberCount").asInt()).isEqualTo(2);
        problem(post(owner.token(), "/api/v1/businesses/{b}/groups/{g}/members", Map.of("customerIds", List.of(outsider)),
                business, group), 404, "not_found");

        assertThat(names(ok(get(owner.token(), CUSTOMERS + "?groupId={g}", business, group), 200))).containsExactly("Aarav", "Bina");
        assertThat(ok(get(owner.token(), CUSTOMERS + "/{c}", business, a), 200).path("groups").get(0).path("name").asString())
                .isEqualTo("Morning batch");
    }

    @Test
    void featuresBelongToModulesThatCanBeSwitchedOff() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Little Steps", "FITNESS");
        // Fitness has no Groups module by default.
        problem(get(owner.token(), "/api/v1/businesses/{id}/groups", business), 404, "module.disabled");
        enableAllModules(owner, business);
        ok(get(owner.token(), "/api/v1/businesses/{id}/groups", business), 200);
    }

    @Test
    void oneCheckInPerDayAndRollCallReplacesThePresentList() {
        setUp();
        long a = createCustomer(owner, business, "Aarav", null);
        long b = createCustomer(owner, business, "Bina", null);
        long group = ok(post(owner.token(), "/api/v1/businesses/{id}/groups", Map.of("name", "Batch"), business), 201).path("id").asLong();
        ok(post(owner.token(), "/api/v1/businesses/{b}/groups/{g}/members", Map.of("customerIds", List.of(a, b)), business, group), 200);
        startMembership(owner, business, a, createPlan(owner, business, "Monthly", "2499", 30));

        JsonNode first = ok(post(owner.token(), "/api/v1/businesses/{id}/attendance", Map.of("customerId", a), business), 200);
        JsonNode again = ok(post(owner.token(), "/api/v1/businesses/{id}/attendance", Map.of("customerId", a), business), 200);
        assertThat(first.path("alreadyCheckedIn").asBoolean()).isFalse();
        assertThat(first.path("covered").asBoolean()).isTrue();
        assertThat(again.path("alreadyCheckedIn").asBoolean()).isTrue();
        assertThat(again.path("id")).isEqualTo(first.path("id"));

        // ATT-1: no running membership is allowed but flagged.
        assertThat(ok(post(owner.token(), "/api/v1/businesses/{id}/attendance", Map.of("customerId", b), business), 200)
                .path("covered").asBoolean()).isFalse();

        JsonNode roll = ok(post(owner.token(), "/api/v1/businesses/{b}/groups/{g}/attendance",
                Map.of("date", "2026-10-20", "present", List.of(b)), business, group), 200);
        assertThat(names(roll, "customerName")).containsExactly("Bina");
        problem(post(owner.token(), "/api/v1/businesses/{b}/groups/{g}/attendance",
                Map.of("date", "2026-10-21", "present", List.of()), business, group), 400, "validation.failed");
    }

    @Test
    void csvImportAddsGoodRowsAndReportsTheRest() {
        setUp();
        createCustomer(owner, business, "Existing", "9845012873");
        String csv = """
                full_name,phone,email,date_of_birth,joined_on,notes
                Rohan Mehta,98450 00001,rohan@example.com,1990-05-17,2026-01-05,"Likes mornings, early"
                ,98450 00002,,,,
                Bad Email,98450 00003,not-an-email,,,
                Dupe Phone,9845012873,,,,
                Bad Date,98450 00004,,31/02/2020,,
                Priya Nair,98450 00005,,17/05/1992,,
                """;

        JsonNode result = ok(mvc.post().uri(CUSTOMERS + "/import", business)
                .multipart().file(new MockMultipartFile("file", "customers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                .headers(h -> h.setBearerAuth(owner.token())).exchange(), 200);

        assertThat(result.path("added").asInt()).isEqualTo(2);
        assertThat(result.path("skipped").asInt()).isEqualTo(4);
        List<Integer> failedRows = new ArrayList<>();
        result.path("errors").forEach(e -> failedRows.add(e.path("row").asInt()));
        assertThat(failedRows).containsExactly(3, 4, 5, 6);
        assertThat(names(ok(get(owner.token(), CUSTOMERS + "?sort=NUMBER", business), 200))).containsExactly("Priya Nair", "Rohan Mehta", "Existing");
        assertThat(ok(get(owner.token(), CUSTOMERS + "/{c}", business, 2), 200).path("notes").asString()).isEqualTo("Likes mornings, early");
    }

    @Test
    void csvExportNeutralisesFormulas() throws Exception {
        setUp();
        createCustomer(owner, business, "=HYPERLINK(\"http://evil\")", "9845012873");

        MvcTestResult export = get(owner.token(), CUSTOMERS + "/export", business);
        assertThat(export).hasStatus(200);
        String text = export.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿number,full_name");
        assertThat(text).contains("'=HYPERLINK");
        assertThat(export.getResponse().getHeader("Content-Disposition")).contains("customers.csv");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JsonNode search(String term) {
        return ok(get(owner.token(), CUSTOMERS + "?q={q}", business, term), 200);
    }

    private String status(long customerId) {
        return ok(get(owner.token(), CUSTOMERS + "/{c}", business, customerId), 200).path("membershipState").asString();
    }

    private static List<String> names(JsonNode node) {
        return names(node, "fullName");
    }

    private static List<String> names(JsonNode node, String field) {
        JsonNode items = node.has("items") ? node.path("items") : node;
        List<String> names = new ArrayList<>();
        items.forEach(i -> names.add(i.path(field).asString()));
        return names;
    }
}
