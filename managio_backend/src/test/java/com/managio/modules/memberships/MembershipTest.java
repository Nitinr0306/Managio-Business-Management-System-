package com.managio.modules.memberships;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Plans and memberships: MEM-1 to MEM-9. The test clock starts at 10:00 IST on Tuesday 20 October 2026. */
class MembershipTest extends IntegrationTest {

    private static final String MEMBERSHIPS = "/api/v1/businesses/{id}/memberships";

    private Session owner;
    private long business;
    private long rohan;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        rohan = createCustomer(owner, business, "Rohan Mehta", "9845012873");
    }

    @Test
    void dayPlansEndOnTheirLastDayAndMonthPlansOnTheDayBeforeTheAnniversary() {
        setUp();
        long thirtyDays = createPlan(owner, business, "30 days", "2499", 30);
        long oneMonth = createPlan(owner, business, "One month", "2499", 1, "MONTHS", 1);
        long threeMonths = createPlan(owner, business, "Three months", "6499", 3, "MONTHS", 1);

        JsonNode days = startMembership(owner, business, rohan, thirtyDays);
        assertThat(days.path("startsOn").asString()).isEqualTo("2026-10-20");
        assertThat(days.path("endsOn").asString()).isEqualTo("2026-11-18");
        JsonNode month = ok(post(owner.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", oneMonth,
                "startsOn", "2026-12-01"), business), 201);
        assertThat(month.path("endsOn").asString()).isEqualTo("2026-12-31");
        assertThat(month.path("state").asString()).isEqualTo("UPCOMING");
        // 31 Jan + 3 months clamps to 30 April, so the last day is 29 April.
        JsonNode clamped = ok(post(owner.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", threeMonths,
                "startsOn", "2027-01-31"), business), 201);
        assertThat(clamped.path("endsOn").asString()).isEqualTo("2027-04-29");
    }

    @Test
    void startingCreatesTheInvoiceAndAPriceChangeLaterDoesNotTouchIt() {
        setUp();
        long plan = createPlan(owner, business, "Quarterly Strength", "6499", 90);

        JsonNode membership = startMembership(owner, business, rohan, plan);

        assertThat(membership.path("state").asString()).isEqualTo("ACTIVE");
        assertThat(membership.path("invoices")).hasSize(1);
        JsonNode invoice = membership.path("invoices").get(0);
        assertThat(invoice.path("total").decimalValue()).isEqualByComparingTo("6499");
        assertThat(invoice.path("dueOn").asString()).isEqualTo("2026-10-20");
        assertThat(invoice.path("state").asString()).isEqualTo("DUE");
        assertThat(invoice.path("invoiceNumber").asString()).isEqualTo("INV-0001");

        // MEM-7: changing the price affects future sales only.
        ok(patch(owner.token(), "/api/v1/businesses/{b}/offerings/{o}", Map.of("kind", "PLAN", "name", "Quarterly Strength",
                "price", "7999", "durationValue", 90, "durationUnit", "DAYS"), business, plan), 200);
        JsonNode again = ok(get(owner.token(), MEMBERSHIPS + "/{m}", business, membership.path("id").asLong()), 200);
        assertThat(again.path("price").decimalValue()).isEqualByComparingTo("6499");
        assertThat(again.path("invoices").get(0).path("total").decimalValue()).isEqualByComparingTo("6499");
    }

    @Test
    void installmentsSplitThePriceAndSpreadTheDueDates() {
        setUp();
        long plan = createPlan(owner, business, "Annual", "10000", 3, "MONTHS", 3);

        JsonNode membership = startMembership(owner, business, rohan, plan);

        List<BigDecimal> totals = new ArrayList<>();
        List<String> due = new ArrayList<>();
        membership.path("invoices").forEach(i -> {
            totals.add(i.path("total").decimalValue());
            due.add(i.path("dueOn").asString());
        });
        assertThat(totals).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("3333.33"), new BigDecimal("3333.33"), new BigDecimal("3333.34"));
        // 20 Oct to 19 Jan is 92 days, so installments fall on days 0, 30 and 61.
        assertThat(due).containsExactly("2026-10-20", "2026-11-19", "2026-12-20");
        assertThat(membership.path("invoices").get(1).path("invoiceNumber").asString()).isEqualTo("INV-0002");
        // BIL-10: only invoices due today or earlier count as owed; later installments are upcoming.
        JsonNode customer = ok(get(owner.token(), "/api/v1/businesses/{b}/customers/{c}", business, rohan), 200);
        assertThat(customer.path("balanceDue").decimalValue()).isEqualByComparingTo("3333.33");
        assertThat(customer.path("upcoming").decimalValue()).isEqualByComparingTo("6666.67");
    }

    @Test
    void aDiscountAppliesToTheFirstInvoiceOnly() {
        setUp();
        long plan = createPlan(owner, business, "Annual", "6000", 90, "DAYS", 3);

        JsonNode membership = ok(post(owner.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", plan,
                "discount", "500"), business), 201);

        List<BigDecimal> totals = new ArrayList<>();
        membership.path("invoices").forEach(i -> totals.add(i.path("total").decimalValue()));
        assertThat(totals).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1500"), new BigDecimal("2000"), new BigDecimal("2000"));
    }

    @Test
    void thePricePlusTaxIsInvoiced() {
        setUp();
        long plan = ok(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PLAN", "name", "Taxed",
                "price", "1000", "taxRate", "18", "durationValue", 30, "durationUnit", "DAYS"), business), 201).path("id").asLong();

        JsonNode invoice = startMembership(owner, business, rohan, plan).path("invoices").get(0);

        assertThat(invoice.path("subtotal").decimalValue()).isEqualByComparingTo("1000");
        assertThat(invoice.path("tax").decimalValue()).isEqualByComparingTo("180");
        assertThat(invoice.path("total").decimalValue()).isEqualByComparingTo("1180");
    }

    @Test
    void overlappingMembershipsOfTheSamePlanAreRefused() {
        setUp();
        long plan = createPlan(owner, business, "Monthly", "2499", 30);
        long other = createPlan(owner, business, "Yoga", "1999", 30);
        startMembership(owner, business, rohan, plan);

        problem(post(owner.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", plan), business), 409, "membership.overlap");
        // A different plan, or a start after it ends, is fine (MEM-5).
        startMembership(owner, business, rohan, other);
        ok(post(owner.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", plan, "startsOn", "2026-11-19"), business), 201);
    }

    @Test
    void renewingContinuesAfterTheEndDateOrStartsTodayWhenExpired() {
        setUp();
        long plan = createPlan(owner, business, "Monthly", "2499", 30);
        long membership = startMembership(owner, business, rohan, plan).path("id").asLong();

        JsonNode early = ok(post(owner.token(), MEMBERSHIPS + "/{m}/renew", Map.of(), business, membership), 201);
        assertThat(early.path("startsOn").asString()).isEqualTo("2026-11-19");
        assertThat(early.path("endsOn").asString()).isEqualTo("2026-12-18");
        assertThat(early.path("renewedFromId").asLong()).isEqualTo(membership);
        problem(post(owner.token(), MEMBERSHIPS + "/{m}/renew", Map.of(), business, membership), 422, "membership.already_renewed");

        // MEM-3: after the last membership expires, a renewal starts today.
        clock.advance(Duration.ofDays(70));
        owner = reauth(owner);
        JsonNode late = ok(post(owner.token(), MEMBERSHIPS + "/{m}/renew", Map.of(), business, early.path("id").asLong()), 201);
        assertThat(late.path("startsOn").asString()).isEqualTo("2026-12-29");
    }

    @Test
    void renewalWorklistShowsWhoToChase() {
        setUp();
        long plan = createPlan(owner, business, "Short", "500", 4);
        long priya = createCustomer(owner, business, "Priya Nair", null);
        long meera = createCustomer(owner, business, "Meera Pillai", null);
        long priyaMembership = startMembership(owner, business, priya, plan).path("id").asLong();
        startMembership(owner, business, meera, plan);
        startMembership(owner, business, rohan, createPlan(owner, business, "Long", "6499", 90));

        assertThat(rows("view=ENDING_SOON&needsRenewal=true")).containsExactly("Priya Nair", "Meera Pillai");
        ok(post(owner.token(), MEMBERSHIPS + "/{m}/renew", Map.of(), business, priyaMembership), 201);
        assertThat(rows("view=ENDING_SOON&needsRenewal=true")).containsExactly("Meera Pillai");

        clock.advance(Duration.ofDays(5));
        owner = reauth(owner);
        assertThat(rows("view=EXPIRED&needsRenewal=true")).containsExactly("Meera Pillai");
        assertThat(rows("view=ACTIVE")).containsExactlyInAnyOrder("Rohan Mehta", "Priya Nair");
    }

    @Test
    void cancellingVoidsUnpaidFutureInvoicesButKeepsWhatIsOwed() {
        setUp();
        long plan = createPlan(owner, business, "Annual", "9000", 90, "DAYS", 3);
        JsonNode membership = startMembership(owner, business, rohan, plan);
        long id = membership.path("id").asLong();
        long firstInvoice = membership.path("invoices").get(0).path("id").asLong();
        ok(post(owner.token(), "/api/v1/businesses/{b}/invoices/{i}/payments", Map.of("method", "CASH", "amount", "1000"),
                business, firstInvoice), 201);

        JsonNode cancelled = ok(post(owner.token(), MEMBERSHIPS + "/{m}/cancel", Map.of("reason", "Moved city"), business, id), 200);

        assertThat(cancelled.path("state").asString()).isEqualTo("CANCELLED");
        List<String> states = new ArrayList<>();
        cancelled.path("invoices").forEach(i -> states.add(i.path("state").asString()));
        // The first invoice (due today, part paid) stays; the two later ones were unpaid and not yet due.
        assertThat(states).containsExactly("PARTLY_PAID", "VOID", "VOID");
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/customers/{c}", business, rohan), 200)
                .path("membershipState").asString()).isEqualTo("NONE");
        problem(post(owner.token(), MEMBERSHIPS + "/{m}/cancel", Map.of(), business, id), 422, "membership.cancelled");
        problem(post(owner.token(), MEMBERSHIPS + "/{m}/renew", Map.of(), business, id), 422, "membership.cancelled");
    }

    @Test
    void startingAndCollectingInOneStepNeedsBothPermissions() {
        setUp();
        long plan = createPlan(owner, business, "Monthly", "2499", 30);
        Session frontDesk = joinAs(owner, business, roleId(owner, business, "FRONT_DESK"), "Divya", "divya@example.com");
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");

        problem(post(trainer.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", plan), business), 403, "auth.forbidden");
        JsonNode paid = ok(post(frontDesk.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", plan,
                "payNow", Map.of("method", "UPI", "reference", "UPI-123")), business), 201);

        JsonNode invoice = paid.path("invoices").get(0);
        assertThat(invoice.path("state").asString()).isEqualTo("PAID");
        assertThat(invoice.path("payments").get(0).path("receiptNumber").asString()).isEqualTo("RCT-0001");
        assertThat(invoice.path("payments").get(0).path("receivedBy").asString()).isEqualTo("Divya");
        // MEM-9 / BIL-4: front desk may not give discounts.
        long other = createPlan(owner, business, "Yoga", "1999", 30);
        problem(post(frontDesk.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", other, "discount", "100"), business),
                403, "auth.forbidden");
    }

    @Test
    void plansNeedTheMembershipsModuleAndRealPlans() {
        setUp();
        long plan = createPlan(owner, business, "Monthly", "2499", 30);
        ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("CATALOG"), "timezone", "Asia/Kolkata",
                "invoicePrefix", "INV", "receiptPrefix", "RCT"), business), 200);

        problem(post(owner.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", plan), business), 404, "module.disabled");
        problem(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PLAN", "name", "X", "price", "1",
                "durationValue", 30, "durationUnit", "DAYS"), business), 422, "module.disabled");
    }

    @Test
    void membershipsOfOtherBusinessesAreUnreachable() {
        setUp();
        long plan = createPlan(owner, business, "Monthly", "2499", 30);
        long membership = startMembership(owner, business, rohan, plan).path("id").asLong();
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long other = createBusiness(rahul, "Prana Yoga", "FITNESS");
        long otherCustomer = createCustomer(rahul, other, "Sameer", null);

        problem(get(rahul.token(), MEMBERSHIPS + "/{m}", other, membership), 404, "not_found");
        problem(post(rahul.token(), MEMBERSHIPS + "/{m}/cancel", Map.of(), other, membership), 404, "not_found");
        problem(post(rahul.token(), MEMBERSHIPS, Map.of("customerId", otherCustomer, "offeringId", plan), other), 404, "not_found");
        problem(post(rahul.token(), MEMBERSHIPS, Map.of("customerId", rohan, "offeringId", createPlan(rahul, other, "P", "10", 30)), other),
                404, "not_found");
    }

    private List<String> rows(String query) {
        List<String> names = new ArrayList<>();
        ok(get(owner.token(), MEMBERSHIPS + "?" + query, business), 200).path("items")
                .forEach(i -> names.add(i.path("customerName").asString()));
        return names;
    }
}
