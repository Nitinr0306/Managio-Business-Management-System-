package com.managio.modules.reports;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The Today view, renewal runway, profit and loss (REP-1) and the portfolio (REP-2). */
class ReportsTest extends IntegrationTest {

    private static final String B = "/api/v1/businesses/{b}";

    private Session owner;
    private long business;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
    }

    /** A person who can see customers and nothing about money. */
    private Session viewer() {
        long role = ok(post(owner.token(), B + "/roles", Map.of("name", "Viewer", "permissions", List.of("CUSTOMERS_VIEW")), business), 201)
                .path("id").asLong();
        return joinAs(owner, business, role, "Vik", "vik@example.com");
    }

    private long category(String name) {
        for (JsonNode c : ok(get(owner.token(), B + "/expense-categories", business), 200)) {
            if (name.equals(c.path("name").asString())) {
                return c.path("id").asLong();
            }
        }
        throw new AssertionError("No category " + name);
    }

    private long invoice(String amount) {
        long customer = createCustomer(owner, business, "Rohan Mehta", null);
        return ok(post(owner.token(), B + "/invoices", Map.of("customerId", customer,
                "lines", List.of(Map.of("description", "Fee", "unitPrice", amount))), business), 201).path("id").asLong();
    }

    @Test
    void theRunwayCountsMembershipsEndingEachDayUntilTheyAreRenewed() {
        setUp();
        long short_ = createPlan(owner, business, "Trial", "300", 3);
        long month = createPlan(owner, business, "Monthly", "1500", 30);
        long asha = createCustomer(owner, business, "Asha", null);
        long bela = createCustomer(owner, business, "Bela", null);
        long chitra = createCustomer(owner, business, "Chitra", null);
        long ashaMembership = startMembership(owner, business, asha, short_).path("id").asLong();
        startMembership(owner, business, bela, short_);
        startMembership(owner, business, chitra, month);

        JsonNode runway = ok(get(owner.token(), B + "/reports/runway?days=30", business), 200);
        assertThat(runway.path("days")).hasSize(30);
        assertThat(runway.path("from").asString()).isEqualTo("2026-10-20");
        assertThat(runway.path("to").asString()).isEqualTo("2026-11-18");
        JsonNode trialEnds = runway.path("days").get(2);
        assertThat(trialEnds.path("date").asString()).isEqualTo("2026-10-22");
        assertThat(trialEnds.path("ending").asLong()).isEqualTo(2);
        assertThat(trialEnds.path("value").decimalValue()).isEqualByComparingTo("600");
        assertThat(runway.path("days").get(29).path("ending").asLong()).isEqualTo(1);
        assertThat(runway.path("days").get(10).path("ending").asLong()).isZero();

        // Renewing takes the person off the runway.
        ok(post(owner.token(), B + "/memberships/{m}/renew", Map.of(), business, ashaMembership), 201);
        assertThat(ok(get(owner.token(), B + "/reports/runway?days=30", business), 200).path("days").get(2).path("ending").asLong()).isEqualTo(1);

        // Whoever was not renewed shows as lapsed once the end date has passed.
        clock.advance(Duration.ofDays(4));
        owner = reauth(owner);
        JsonNode later = ok(get(owner.token(), B + "/reports/runway?days=7", business), 200);
        assertThat(later.path("lapsedLast30Days").asLong()).isEqualTo(1);
        assertThat(later.path("lapsedValue").decimalValue()).isEqualByComparingTo("300");

        problem(get(owner.token(), B + "/reports/runway?days=0", business), 400, "validation.failed");
        problem(get(owner.token(), B + "/reports/runway?days=91", business), 400, "validation.failed");
    }

    @Test
    void profitAndLossIsCashBasedAndIgnoresVoidedRecords() {
        setUp();
        long invoice = invoice("1000");
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "600", "method", "CASH"), business, invoice), 201);
        long upi = ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "400", "method", "UPI"), business, invoice), 201).path("id").asLong();
        long cash = ok(get(owner.token(), B + "/payments?method=CASH", business), 200).path("items").get(0).path("id").asLong();
        ok(post(owner.token(), B + "/payments/{p}/refunds", Map.of("amount", "100", "method", "CASH", "reason", "Changed mind"), business, cash), 201);
        ok(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Supplies"), "amount", "150", "method", "CASH"), business), 201);
        ok(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Rent"), "amount", "300", "method", "UPI"), business), 201);
        long wrong = ok(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Rent"), "amount", "999", "method", "UPI"), business), 201)
                .path("id").asLong();
        ok(post(owner.token(), B + "/expenses/{e}/void", Map.of("reason", "Typo"), business, wrong), 200);

        JsonNode pl = ok(get(owner.token(), B + "/reports/profit-loss", business), 200);
        assertThat(pl.path("from").asString()).isEqualTo("2026-10-01");
        assertThat(pl.path("to").asString()).isEqualTo("2026-10-20");
        assertThat(pl.path("granularity").asString()).isEqualTo("DAY");
        assertThat(pl.path("revenue").decimalValue()).isEqualByComparingTo("900");
        assertThat(pl.path("expenses").decimalValue()).isEqualByComparingTo("450");
        assertThat(pl.path("profit").decimalValue()).isEqualByComparingTo("450");
        assertThat(pl.path("revenueByMethod").path("CASH").decimalValue()).isEqualByComparingTo("500");
        assertThat(pl.path("revenueByMethod").path("UPI").decimalValue()).isEqualByComparingTo("400");
        assertThat(pl.path("expensesByCategory").get(0).path("category").asString()).isEqualTo("Rent");
        assertThat(pl.path("series")).hasSize(1);

        // Voiding a payment takes it out of revenue.
        ok(post(owner.token(), B + "/payments/{p}/void", Map.of("reason", "Bounced"), business, upi), 200);
        assertThat(ok(get(owner.token(), B + "/reports/profit-loss", business), 200).path("revenue").decimalValue()).isEqualByComparingTo("500");

        // Long ranges group by month.
        clock.advance(Duration.ofDays(16));
        owner = reauth(owner);
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "100", "method", "UPI"), business, invoice), 201);
        JsonNode yearly = ok(get(owner.token(), B + "/reports/profit-loss?from=2026-10-01&to=2026-12-31", business), 200);
        assertThat(yearly.path("granularity").asString()).isEqualTo("MONTH");
        assertThat(yearly.path("series")).hasSize(2);
        assertThat(yearly.path("series").get(0).path("bucket").asString()).isEqualTo("2026-10-01");
        assertThat(yearly.path("series").get(1).path("revenue").decimalValue()).isEqualByComparingTo("100");
        assertThat(yearly.path("revenue").decimalValue()).isEqualByComparingTo("600");

        problem(get(owner.token(), B + "/reports/profit-loss?from=2026-11-05&to=2026-11-01", business), 400, "validation.failed");
        problem(get(owner.token(), B + "/reports/profit-loss?from=2025-01-01&to=2026-11-01", business), 400, "validation.failed");
    }

    @Test
    void reportsNeedThePermissionAndStayInsideTheBusiness() {
        setUp();
        Session viewer = viewer();
        problem(get(viewer.token(), B + "/reports/profit-loss", business), 403, "auth.forbidden");
        ok(get(viewer.token(), B + "/reports/runway", business), 200);

        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");
        problem(get(rahul.token(), B + "/reports/profit-loss", business), 404, "not_found");
        problem(get(rahul.token(), B + "/today", business), 404, "not_found");
        // Ironwood's money does not leak into the Mart's figures.
        long invoice = invoice("500");
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "500", "method", "CASH"), business, invoice), 201);
        assertThat(ok(get(rahul.token(), B + "/reports/profit-loss", mart), 200).path("revenue").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void todayShowsOnlyWhatTheRoleMaySee() {
        setUp();
        long oneDay = createPlan(owner, business, "Day pass", "100", 1);
        long asha = createCustomer(owner, business, "Asha", null);
        startMembership(owner, business, asha, oneDay);
        long invoice = invoice("1500");
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "600", "method", "CASH"), business, invoice), 201);
        ok(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Supplies"), "amount", "50", "method", "CASH"), business), 201);
        Session viewer = viewer();
        ok(post(viewer.token(), B + "/requests", Map.of("type", "OTHER", "title", "New mats"), business), 201);

        JsonNode today = ok(get(owner.token(), B + "/today", business), 200);
        assertThat(today.path("date").asString()).isEqualTo("2026-10-20");
        assertThat(today.path("renewals").path("endingToday").asLong()).isEqualTo(1);
        assertThat(today.path("renewals").path("endingThisWeek").asLong()).isEqualTo(1);
        assertThat(today.path("dues").path("openInvoices").asLong()).isEqualTo(2);
        assertThat(today.path("dues").path("openAmount").decimalValue()).isEqualByComparingTo("1000");
        assertThat(today.path("dues").path("overdueInvoices").asLong()).isZero();
        assertThat(today.path("takings").path("received").decimalValue()).isEqualByComparingTo("600");
        assertThat(today.path("takings").path("spent").decimalValue()).isEqualByComparingTo("50");
        assertThat(today.path("dayClosed").asBoolean()).isFalse();
        assertThat(today.path("newCustomersThisMonth").asLong()).isEqualTo(2);
        assertThat(today.path("pendingApprovals").asLong()).isEqualTo(1);

        ok(post(owner.token(), B + "/day-book/close", Map.of("countedCash", "550"), business), 200);
        assertThat(ok(get(owner.token(), B + "/today", business), 200).path("dayClosed").asBoolean()).isTrue();

        JsonNode limited = ok(get(viewer.token(), B + "/today", business), 200);
        assertThat(limited.path("dues").isNull()).isTrue();
        assertThat(limited.path("takings").isNull()).isTrue();
        assertThat(limited.path("dayClosed").isNull()).isTrue();
        assertThat(limited.path("pendingApprovals").isNull()).isTrue();
        assertThat(limited.path("renewals").path("endingToday").asLong()).isEqualTo(1);
        assertThat(limited.path("myTasks").path("open").asLong()).isZero();
    }

    @Test
    void overdueDuesAndMyTasksAppearOnceTheirDatesPass() {
        setUp();
        long invoice = invoice("800");
        long ownerMember = ok(get(owner.token(), B + "/team", business), 200).get(0).path("id").asLong();
        ok(post(owner.token(), B + "/tasks", Map.of("title", "Chase dues", "assigneeId", ownerMember, "dueOn", "2026-10-21"), business), 201);
        assertThat(ok(get(owner.token(), B + "/today", business), 200).path("myTasks").path("overdue").asLong()).isZero();

        clock.advance(Duration.ofDays(3));
        owner = reauth(owner);
        JsonNode today = ok(get(owner.token(), B + "/today", business), 200);
        assertThat(today.path("dues").path("overdueInvoices").asLong()).isEqualTo(1);
        assertThat(today.path("dues").path("overdueAmount").decimalValue()).isEqualByComparingTo("800");
        assertThat(today.path("myTasks").path("open").asLong()).isEqualTo(1);
        assertThat(today.path("myTasks").path("overdue").asLong()).isEqualTo(1);
        assertThat(invoice).isPositive();
    }

    @Test
    void thePortfolioComparesBusinessesAndHidesWhatARoleCannotSee() {
        setUp();
        Session viewer = viewer();
        long mart = createBusiness(owner, "Corner Mart", "RETAIL");
        long invoice = invoice("1000");
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "1000", "method", "UPI"), business, invoice), 201);
        ok(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Rent"), "amount", "400", "method", "UPI"), business), 201);

        JsonNode portfolio = ok(get(owner.token(), "/api/v1/me/portfolio"), 200);
        assertThat(portfolio.path("monthStart").asString()).isEqualTo("2026-10-01");
        JsonNode lines = portfolio.path("businesses");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).path("name").asString()).isEqualTo("Corner Mart");
        assertThat(lines.get(0).path("id").asLong()).isEqualTo(mart);
        assertThat(lines.get(0).path("revenueThisMonth").decimalValue()).isEqualByComparingTo("0");
        assertThat(lines.get(1).path("name").asString()).isEqualTo("Ironwood Fitness");
        assertThat(lines.get(1).path("owner").asBoolean()).isTrue();
        assertThat(lines.get(1).path("revenueThisMonth").decimalValue()).isEqualByComparingTo("1000");
        assertThat(lines.get(1).path("profitThisMonth").decimalValue()).isEqualByComparingTo("600");
        assertThat(lines.get(1).path("overdueAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(portfolio.path("totals").path("businessesCounted").asInt()).isEqualTo(2);
        assertThat(portfolio.path("totals").path("profitThisMonth").decimalValue()).isEqualByComparingTo("600");

        // Someone with a limited role sees their business but none of its money.
        JsonNode theirs = ok(get(viewer.token(), "/api/v1/me/portfolio"), 200);
        assertThat(theirs.path("businesses")).hasSize(1);
        assertThat(theirs.path("businesses").get(0).path("revenueThisMonth").isNull()).isTrue();
        assertThat(theirs.path("businesses").get(0).path("overdueAmount").isNull()).isTrue();
        assertThat(theirs.path("totals").path("businessesCounted").asInt()).isZero();
        assertThat(get(null, "/api/v1/me/portfolio")).hasStatus(401);
    }
}
