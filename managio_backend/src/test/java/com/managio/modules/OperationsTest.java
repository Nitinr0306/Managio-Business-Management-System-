package com.managio.modules;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Expenses, payroll, day book, tasks, requests and notifications: EXP-*, PAY-*, DAY-*, TSK-*, REQ-*. */
class OperationsTest extends IntegrationTest {

    private static final String B = "/api/v1/businesses/{b}";

    private Session owner;
    private Session frontDesk;
    private long business;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        frontDesk = joinAs(owner, business, roleId(owner, business, "FRONT_DESK"), "Divya", "divya@example.com");
    }

    // ── Expenses ─────────────────────────────────────────────────────────────

    @Test
    void expensesAreRecordedVoidedAndSummarised() {
        setUp();
        long rent = category("Rent");
        long supplies = category("Supplies");

        JsonNode first = expense(owner, rent, "25000", "BANK_TRANSFER", "Landlord");
        expense(owner, supplies, "800", "CASH", "Cleaning mart");
        assertThat(first.path("category").path("name").asString()).isEqualTo("Rent");
        assertThat(first.path("spentOn").asString()).isEqualTo("2026-10-20");
        assertThat(first.path("recordedBy").asString()).isEqualTo("Ananya Rao");

        JsonNode summary = ok(get(owner.token(), B + "/expenses/summary", business), 200);
        assertThat(summary.path("total").decimalValue()).isEqualByComparingTo("25800");
        assertThat(summary.path("byCategory").get(0).path("category").asString()).isEqualTo("Rent");

        problem(post(owner.token(), B + "/expenses/{e}/void", Map.of("reason", " "), business, first.path("id").asLong()), 400, "validation.failed");
        ok(post(owner.token(), B + "/expenses/{e}/void", Map.of("reason", "Entered twice"), business, first.path("id").asLong()), 200);
        problem(post(owner.token(), B + "/expenses/{e}/void", Map.of("reason", "Again"), business, first.path("id").asLong()), 422, "expense.already_void");
        assertThat(ok(get(owner.token(), B + "/expenses/summary", business), 200).path("total").decimalValue()).isEqualByComparingTo("800");
        assertThat(ok(get(owner.token(), B + "/expenses", business), 200).path("totalItems").asInt()).isEqualTo(1);
        assertThat(ok(get(owner.token(), B + "/expenses?includeVoid=true", business), 200).path("totalItems").asInt()).isEqualTo(2);
        assertThat(ok(get(owner.token(), B + "/expenses?q=cleaning", business), 200).path("totalItems").asInt()).isEqualTo(1);
    }

    @Test
    void expenseValidationPermissionsAndCategories() {
        setUp();
        long rent = category("Rent");
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");

        problem(post(owner.token(), B + "/expenses", Map.of("categoryId", rent, "amount", "0", "method", "CASH"), business), 400, "validation.failed");
        problem(post(owner.token(), B + "/expenses", Map.of("categoryId", rent, "amount", "10", "method", "CASH",
                "spentOn", "2026-10-21"), business), 400, "validation.failed");
        problem(post(frontDesk.token(), B + "/expenses", Map.of("categoryId", rent, "amount", "10", "method", "CASH"), business), 403, "auth.forbidden");
        problem(get(frontDesk.token(), B + "/expenses", business), 403, "auth.forbidden");

        // Only people with settings.manage change categories; the Salaries category can't be archived.
        problem(post(manager.token(), B + "/expense-categories", Map.of("name", "Gym equipment"), business), 403, "auth.forbidden");
        // Anyone on the team can read them, because an expense claim needs a category (REQ-1).
        assertThat(ok(get(frontDesk.token(), B + "/expense-categories", business), 200)).isNotEmpty();
        long equipment = ok(post(owner.token(), B + "/expense-categories", Map.of("name", "Gym equipment"), business), 201).path("id").asLong();
        ok(patch(owner.token(), B + "/expense-categories/{c}", Map.of("name", "Gym equipment", "archived", true), business, equipment), 200);
        problem(post(owner.token(), B + "/expenses", Map.of("categoryId", equipment, "amount", "10", "method", "CASH"), business), 422, "category.archived");
        problem(patch(owner.token(), B + "/expense-categories/{c}", Map.of("name", "Salaries", "archived", true), business, category("Salaries")),
                422, "category.required");

        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");
        problem(post(rahul.token(), "/api/v1/businesses/{b}/expenses", Map.of("categoryId", rent, "amount", "10", "method", "CASH"), mart), 404, "not_found");
        long id = expense(owner, rent, "100", "UPI", null).path("id").asLong();
        problem(get(rahul.token(), "/api/v1/businesses/{b}/expenses/{e}", mart, id), 404, "not_found");
    }

    // ── Payroll ──────────────────────────────────────────────────────────────

    @Test
    void salariesArePreparedPaidInPartsAndTrackedThroughExpenses() {
        setUp();
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");
        setSalary(manager, "25000");
        setSalary(frontDesk, "18000");

        List<JsonNode> prepared = list(ok(post(owner.token(), B + "/payroll/prepare", Map.of(), business), 200));
        assertThat(prepared).hasSize(2);
        assertThat(list(ok(post(owner.token(), B + "/payroll/prepare", Map.of(), business), 200))).hasSize(2);
        problem(post(owner.token(), B + "/payroll/prepare", Map.of("month", "2026-11-01"), business), 400, "validation.failed");

        long ravi = entryFor("Ravi");
        JsonNode part = ok(post(owner.token(), B + "/payroll/{e}/payments", Map.of("amount", "10000", "method", "UPI"), business, ravi), 200);
        assertThat(part.path("state").asString()).isEqualTo("PARTLY_PAID");
        assertThat(part.path("remaining").decimalValue()).isEqualByComparingTo("15000");
        problem(post(owner.token(), B + "/payroll/{e}/payments", Map.of("amount", "15000.01", "method", "UPI"), business, ravi), 400, "validation.failed");

        JsonNode paid = ok(post(owner.token(), B + "/payroll/{e}/payments", Map.of("method", "BANK_TRANSFER"), business, ravi), 200);
        assertThat(paid.path("state").asString()).isEqualTo("PAID");
        problem(post(owner.token(), B + "/payroll/{e}/payments", Map.of("method", "CASH"), business, ravi), 400, "validation.failed");

        JsonNode salaryExpenses = ok(get(owner.token(), B + "/expenses?categoryId={c}", business, category("Salaries")), 200);
        assertThat(salaryExpenses.path("totalItems").asInt()).isEqualTo(2);
        assertThat(salaryExpenses.path("items").get(0).path("payee").asString()).isEqualTo("Ravi");
        assertThat(salaryExpenses.path("items").get(0).path("description").asString()).isEqualTo("Salary for Oct 2026");

        // Voiding a salary expense puts the money back on what is still owed.
        long expenseId = salaryExpenses.path("items").get(0).path("id").asLong();
        ok(post(owner.token(), B + "/expenses/{e}/void", Map.of("reason", "Bounced"), business, expenseId), 200);
        assertThat(entry(ravi).path("state").asString()).isEqualTo("PARTLY_PAID");

        problem(patch(owner.token(), B + "/payroll/{e}", Map.of("amount", "9999"), business, ravi), 400, "validation.failed");
        assertThat(ok(patch(owner.token(), B + "/payroll/{e}", Map.of("amount", "27000", "notes", "Bonus"), business, ravi), 200)
                .path("amount").decimalValue()).isEqualByComparingTo("27000");
    }

    @Test
    void onlyPayrollManagersSeeSalaries() {
        setUp();
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");
        problem(get(manager.token(), B + "/payroll", business), 403, "auth.forbidden");
        problem(post(manager.token(), B + "/payroll/prepare", Map.of(), business), 403, "auth.forbidden");
        ok(get(owner.token(), B + "/payroll", business), 200);

        ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("MEMBERSHIPS"), "timezone", "Asia/Kolkata",
                "invoicePrefix", "INV", "receiptPrefix", "RCT"), business), 200);
        problem(get(owner.token(), B + "/payroll", business), 404, "module.disabled");
    }

    @Test
    void peopleWhoLeftOrHaveNoSalaryAreNotInPayroll() {
        setUp();
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");
        setSalary(manager, "25000");
        long divya = memberId(frontDesk.userId());
        setSalary(frontDesk, "18000");
        ok(post(owner.token(), B + "/team/{m}/remove", null, business, divya), 200);

        assertThat(list(ok(post(owner.token(), B + "/payroll/prepare", Map.of(), business), 200))).hasSize(1);
    }

    // ── Day book ─────────────────────────────────────────────────────────────

    @Test
    void theDayBookCountsCashAndClosingLocksCashForThatDay() {
        setUp();
        long invoice = ok(post(owner.token(), B + "/invoices", Map.of("customerId", customer("Rohan Mehta"),
                "lines", List.of(Map.of("description", "Fee", "unitPrice", "1500"))), business), 201).path("id").asLong();
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "600", "method", "CASH"), business, invoice), 201);
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "400", "method", "UPI"), business, invoice), 201);
        expense(owner, category("Supplies"), "150", "CASH", "Mart");
        expense(owner, category("Rent"), "9000", "UPI", "Landlord");

        JsonNode day = ok(get(frontDesk.token(), B + "/day-book", business), 200);
        assertThat(day.path("openingCash").decimalValue()).isEqualByComparingTo("0");
        assertThat(day.path("cashIn").decimalValue()).isEqualByComparingTo("600");
        assertThat(day.path("cashOut").decimalValue()).isEqualByComparingTo("150");
        assertThat(day.path("expectedCash").decimalValue()).isEqualByComparingTo("450");
        assertThat(day.path("byMethod").path("UPI").decimalValue()).isEqualByComparingTo("400");
        assertThat(day.path("closed").isNull()).isTrue();

        problem(post(frontDesk.token(), B + "/day-book/close", Map.of("date", "2026-10-21", "countedCash", "450"), business), 400, "validation.failed");
        JsonNode closed = ok(post(frontDesk.token(), B + "/day-book/close", Map.of("countedCash", "440", "notes", "Short by a tea"), business), 200);
        assertThat(closed.path("closed").path("difference").decimalValue()).isEqualByComparingTo("-10");
        assertThat(closed.path("closed").path("closedBy").asString()).isEqualTo("Divya");
        problem(post(owner.token(), B + "/day-book/close", Map.of("countedCash", "450"), business), 409, "day.already_closed");

        // DAY-3: cash is locked for the closed day, other methods are not.
        problem(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "1", "method", "CASH"), business, invoice), 422, "day.closed");
        problem(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Rent"), "amount", "5", "method", "CASH"), business), 422, "day.closed");
        ok(post(owner.token(), B + "/expenses", Map.of("categoryId", category("Rent"), "amount", "5", "method", "UPI"), business), 201);
        JsonNode cashPayment = ok(get(owner.token(), B + "/payments?method=CASH", business), 200).path("items").get(0);
        problem(post(owner.token(), B + "/payments/{p}/void", Map.of("reason", "x"), business, cashPayment.path("id").asLong()), 422, "day.closed");
        problem(post(owner.token(), B + "/payments/{p}/refunds", Map.of("amount", "10", "method", "CASH", "reason", "x"), business,
                cashPayment.path("id").asLong()), 422, "day.closed");

        // The next day starts from the counted cash, not the expected cash.
        clock.advance(Duration.ofDays(1));
        owner = reauth(owner);
        assertThat(ok(get(owner.token(), B + "/day-book", business), 200).path("openingCash").decimalValue()).isEqualByComparingTo("440");
        assertThat(ok(get(owner.token(), B + "/day-book/history", business), 200)).hasSize(1);
    }

    @Test
    void tenantsCannotSeeOrCloseEachOthersDays() {
        setUp();
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");

        problem(get(trainer.token(), B + "/day-book", business), 403, "auth.forbidden");
        problem(get(rahul.token(), "/api/v1/businesses/{b}/day-book", business), 404, "not_found");
        ok(post(rahul.token(), "/api/v1/businesses/{b}/day-book/close", Map.of("countedCash", "0"), mart), 200);
        // Closing the Mart's day doesn't lock Ironwood's.
        ok(post(owner.token(), B + "/day-book/close", Map.of("countedCash", "0"), business), 200);
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    @Test
    void tasksFollowWhoOwnsThem() {
        setUp();
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");
        long divya = memberId(frontDesk.userId());
        long kiran = memberId(trainer.userId());
        long task = ok(post(owner.token(), B + "/tasks", Map.of("title", "Call lapsed members", "assigneeId", divya,
                "priority", "HIGH", "dueOn", "2026-10-19"), business), 201).path("id").asLong();

        // The assignee sees it, can move it along but not edit it; others don't see it.
        JsonNode mine = ok(get(frontDesk.token(), B + "/tasks?mine=true", business), 200);
        assertThat(mine.path("items")).hasSize(1);
        assertThat(mine.path("items").get(0).path("overdue").asBoolean()).isTrue();
        problem(put(frontDesk.token(), B + "/tasks/{t}", Map.of("title", "Renamed"), business, task), 403, "auth.forbidden");
        assertThat(ok(patch(frontDesk.token(), B + "/tasks/{t}/status", Map.of("status", "IN_PROGRESS"), business, task), 200)
                .path("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(ok(get(trainer.token(), B + "/tasks", business), 200).path("items")).isEmpty();
        problem(get(trainer.token(), B + "/tasks/{t}", business, task), 404, "not_found");
        problem(patch(trainer.token(), B + "/tasks/{t}/status", Map.of("status", "DONE"), business, task), 403, "auth.forbidden");

        // TSK-1: without tasks.manage a task can only be for yourself.
        problem(post(trainer.token(), B + "/tasks", Map.of("title", "Do my thing", "assigneeId", divya), business), 403, "auth.forbidden");
        long own = ok(post(trainer.token(), B + "/tasks", Map.of("title", "Plan classes"), business), 201).path("id").asLong();
        assertThat(ok(get(trainer.token(), B + "/tasks/{t}", business, own), 200).path("assignee").path("id").asLong()).isEqualTo(kiran);

        ok(patch(frontDesk.token(), B + "/tasks/{t}/status", Map.of("status", "DONE"), business, task), 200);
        assertThat(ok(get(owner.token(), B + "/tasks?view=ALL", business), 200).path("items")).hasSize(2);
        assertThat(ok(get(owner.token(), B + "/tasks?view=DONE", business), 200).path("items")).hasSize(1);
        assertThat(ok(put(owner.token(), B + "/tasks/{t}", Map.of("title", "Plan classes better", "priority", "LOW"), business, own), 200)
                .path("title").asString()).isEqualTo("Plan classes better");
        assertThat(delete(owner.token(), B + "/tasks/{t}", business, own)).hasStatus(204);
    }

    @Test
    void finishingARepeatingTaskCreatesTheNextOne() {
        setUp();
        long divya = memberId(frontDesk.userId());
        long daily = ok(post(owner.token(), B + "/tasks", Map.of("title", "Open up", "assigneeId", divya, "repeat", "DAILY",
                "dueOn", "2026-10-20"), business), 201).path("id").asLong();
        long monthly = ok(post(owner.token(), B + "/tasks", Map.of("title", "Pay rent", "assigneeId", divya, "repeat", "MONTHLY",
                "dueOn", "2026-10-31"), business), 201).path("id").asLong();

        ok(patch(frontDesk.token(), B + "/tasks/{t}/status", Map.of("status", "DONE"), business, daily), 200);
        ok(patch(frontDesk.token(), B + "/tasks/{t}/status", Map.of("status", "DONE"), business, monthly), 200);
        // Setting an already-finished task to DONE again must not create a third one.
        ok(patch(frontDesk.token(), B + "/tasks/{t}/status", Map.of("status", "DONE"), business, daily), 200);

        List<String> open = new ArrayList<>();
        ok(get(frontDesk.token(), B + "/tasks?view=OPEN&mine=true", business), 200).path("items")
                .forEach(t -> open.add(t.path("title").asString() + " " + t.path("dueOn").asString()));
        assertThat(open).containsExactly("Open up 2026-10-21", "Pay rent 2026-11-30");
    }

    @Test
    void tasksCanOnlyBeAssignedToActiveTeamMembersOfTheSameBusiness() {
        setUp();
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");
        long outsider = ok(get(rahul.token(), "/api/v1/businesses/{b}/team", mart), 200).get(0).path("id").asLong();
        long divya = memberId(frontDesk.userId());
        ok(post(owner.token(), B + "/team/{m}/suspend", null, business, divya), 200);

        problem(post(owner.token(), B + "/tasks", Map.of("title", "X", "assigneeId", outsider), business), 404, "not_found");
        problem(post(owner.token(), B + "/tasks", Map.of("title", "X", "assigneeId", divya), business), 404, "not_found");
        problem(post(owner.token(), B + "/tasks", Map.of("title", " "), business), 400, "validation.failed");
    }

    // ── Requests and notifications ───────────────────────────────────────────

    @Test
    void anApprovedExpenseClaimBecomesAnExpenseAndNotifiesBothSides() {
        setUp();
        long supplies = category("Supplies");
        long request = ok(post(frontDesk.token(), B + "/requests", Map.of("type", "EXPENSE", "title", "Cleaning supplies",
                "amount", "800", "expenseCategoryId", supplies), business), 201).path("id").asLong();

        JsonNode ownerInbox = ok(get(owner.token(), "/api/v1/me/notifications?unread=true"), 200);
        assertThat(ownerInbox).hasSize(1);
        assertThat(ownerInbox.get(0).path("kind").asString()).isEqualTo("REQUEST_NEW");
        assertThat(ok(get(owner.token(), "/api/v1/me/notifications/count"), 200).path("unread").asLong()).isEqualTo(1);

        problem(post(frontDesk.token(), B + "/requests/{r}/approve", Map.of(), business, request), 403, "auth.forbidden");
        problem(post(owner.token(), B + "/requests/{r}/approve", Map.of(), business, request), 400, "validation.failed");
        JsonNode approved = ok(post(owner.token(), B + "/requests/{r}/approve", Map.of("method", "UPI", "note", "Fine"), business, request), 200);

        assertThat(approved.path("status").asString()).isEqualTo("APPROVED");
        assertThat(approved.path("decidedBy").asString()).isEqualTo("Ananya Rao");
        JsonNode expenses = ok(get(owner.token(), B + "/expenses", business), 200).path("items");
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).path("payee").asString()).isEqualTo("Divya");
        assertThat(expenses.get(0).path("requestId").asLong()).isEqualTo(request);
        assertThat(approved.path("expenseId").asLong()).isEqualTo(expenses.get(0).path("id").asLong());

        JsonNode divyaInbox = ok(get(frontDesk.token(), "/api/v1/me/notifications"), 200);
        assertThat(divyaInbox.get(0).path("title").asString()).isEqualTo("Your request was approved");
        problem(post(owner.token(), B + "/requests/{r}/reject", Map.of("note", "Too late"), business, request), 422, "request.closed");

        assertThat(post(owner.token(), "/api/v1/me/notifications/read", null)).hasStatus(204);
        assertThat(ok(get(owner.token(), "/api/v1/me/notifications/count"), 200).path("unread").asLong()).isZero();
        assertThat(ok(get(frontDesk.token(), "/api/v1/me/notifications/count"), 200).path("unread").asLong()).isEqualTo(1);
    }

    @Test
    void nobodyDecidesTheirOwnRequestAndRejectionsNeedAReason() {
        setUp();
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");
        long leave = ok(post(manager.token(), B + "/requests", Map.of("type", "LEAVE", "title", "Family wedding",
                "startsOn", "2026-11-02", "endsOn", "2026-11-04"), business), 201).path("id").asLong();

        problem(post(manager.token(), B + "/requests/{r}/approve", Map.of(), business, leave), 422, "request.own");
        problem(post(owner.token(), B + "/requests/{r}/reject", Map.of("note", " "), business, leave), 400, "validation.failed");
        JsonNode rejected = ok(post(owner.token(), B + "/requests/{r}/reject", Map.of("note", "Month-end audit that week"), business, leave), 200);
        assertThat(rejected.path("status").asString()).isEqualTo("REJECTED");
        assertThat(ok(get(manager.token(), "/api/v1/me/notifications"), 200).get(0).path("body").asString()).contains("Month-end audit");

        long other = ok(post(manager.token(), B + "/requests", Map.of("type", "OTHER", "title", "New speakers"), business), 201).path("id").asLong();
        assertThat(ok(post(owner.token(), B + "/requests/{r}/approve", Map.of(), business, other), 200).path("status").asString()).isEqualTo("APPROVED");
        assertThat(ok(get(owner.token(), B + "/expenses", business), 200).path("items")).isEmpty();
    }

    @Test
    void requestsAreCheckedPerTypeAndCancelledOnlyByTheRequester() {
        setUp();
        problem(post(frontDesk.token(), B + "/requests", Map.of("type", "LEAVE", "title", "Off"), business), 400, "validation.failed");
        problem(post(frontDesk.token(), B + "/requests", Map.of("type", "LEAVE", "title", "Off", "startsOn", "2026-11-05",
                "endsOn", "2026-11-01"), business), 400, "validation.failed");
        problem(post(frontDesk.token(), B + "/requests", Map.of("type", "EXPENSE", "title", "Tea", "amount", "50"), business), 400, "validation.failed");
        problem(post(frontDesk.token(), B + "/requests", Map.of("type", "PURCHASE", "title", "Mats"), business), 400, "validation.failed");

        long request = ok(post(frontDesk.token(), B + "/requests", Map.of("type", "PURCHASE", "title", "Yoga mats", "amount", "4000"), business), 201)
                .path("id").asLong();
        problem(post(owner.token(), B + "/requests/{r}/cancel", null, business, request), 404, "not_found");
        assertThat(ok(post(frontDesk.token(), B + "/requests/{r}/cancel", null, business, request), 200).path("status").asString()).isEqualTo("CANCELLED");
        problem(post(owner.token(), B + "/requests/{r}/approve", Map.of(), business, request), 422, "request.closed");
    }

    @Test
    void requestsAreOnlyVisibleToTheRequesterAndApprovers() {
        setUp();
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");
        long request = ok(post(frontDesk.token(), B + "/requests", Map.of("type", "OTHER", "title", "Idea"), business), 201).path("id").asLong();
        ok(post(trainer.token(), B + "/requests", Map.of("type", "OTHER", "title", "Another idea"), business), 201);

        assertThat(ok(get(frontDesk.token(), B + "/requests", business), 200).path("items")).hasSize(1);
        assertThat(ok(get(owner.token(), B + "/requests", business), 200).path("items")).hasSize(2);
        assertThat(ok(get(owner.token(), B + "/requests?mine=true", business), 200).path("items")).isEmpty();
        problem(get(trainer.token(), B + "/requests/{r}", business, request), 404, "not_found");
        ok(get(owner.token(), B + "/requests/{r}", business, request), 200);
        assertThat(ok(get(owner.token(), B + "/requests/pending-count", business), 200).path("pending").asLong()).isEqualTo(2);
        problem(get(trainer.token(), B + "/requests/pending-count", business), 403, "auth.forbidden");

        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");
        problem(get(rahul.token(), "/api/v1/businesses/{b}/requests/{r}", mart, request), 404, "not_found");
    }

    @Test
    void anApprovedDiscountReducesAnUnpaidInvoice() {
        setUp();
        long invoice = ok(post(owner.token(), B + "/invoices", Map.of("customerId", customer("Rohan Mehta"),
                "lines", List.of(Map.of("description", "Fee", "unitPrice", "1000"))), business), 201).path("id").asLong();

        problem(post(frontDesk.token(), B + "/requests", Map.of("type", "DISCOUNT", "title", "Loyal member", "amount", "2000",
                "invoiceId", invoice), business), 400, "validation.failed");
        long request = ok(post(frontDesk.token(), B + "/requests", Map.of("type", "DISCOUNT", "title", "Loyal member", "amount", "100",
                "invoiceId", invoice), business), 201).path("id").asLong();
        ok(post(owner.token(), B + "/requests/{r}/approve", Map.of(), business, request), 200);

        JsonNode discounted = ok(get(owner.token(), B + "/invoices/{i}", business, invoice), 200);
        assertThat(discounted.path("discount").decimalValue()).isEqualByComparingTo("100");
        assertThat(discounted.path("total").decimalValue()).isEqualByComparingTo("900");

        // A discount can't be approved once money has been taken, and the request stays open.
        long second = ok(post(frontDesk.token(), B + "/requests", Map.of("type", "DISCOUNT", "title", "More", "amount", "50",
                "invoiceId", invoice), business), 201).path("id").asLong();
        ok(post(owner.token(), B + "/invoices/{i}/payments", Map.of("amount", "100", "method", "UPI"), business, invoice), 201);
        problem(post(owner.token(), B + "/requests/{r}/approve", Map.of(), business, second), 422, "invoice.not_discountable");
        assertThat(ok(get(owner.token(), B + "/requests/{r}", business, second), 200).path("status").asString()).isEqualTo("PENDING");
    }

    @Test
    void approvingACashExpenseOnAClosedDayFailsAndLeavesTheRequestPending() {
        setUp();
        long request = ok(post(frontDesk.token(), B + "/requests", Map.of("type", "EXPENSE", "title", "Tea", "amount", "50",
                "expenseCategoryId", category("Supplies")), business), 201).path("id").asLong();
        ok(post(owner.token(), B + "/day-book/close", Map.of("countedCash", "0"), business), 200);

        problem(post(owner.token(), B + "/requests/{r}/approve", Map.of("method", "CASH"), business, request), 422, "day.closed");
        assertThat(ok(get(owner.token(), B + "/requests/{r}", business, request), 200).path("status").asString()).isEqualTo("PENDING");
        assertThat(ok(get(owner.token(), B + "/expenses", business), 200).path("items")).isEmpty();
        ok(post(owner.token(), B + "/requests/{r}/approve", Map.of("method", "UPI"), business, request), 200);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long category(String name) {
        for (JsonNode c : ok(get(owner.token(), B + "/expense-categories", business), 200)) {
            if (name.equals(c.path("name").asString())) {
                return c.path("id").asLong();
            }
        }
        throw new AssertionError("No category " + name);
    }

    private JsonNode expense(Session session, long categoryId, String amount, String method, String payee) {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("categoryId", categoryId, "amount", amount, "method", method));
        if (payee != null) {
            body.put("payee", payee);
        }
        return ok(post(session.token(), B + "/expenses", body, business), 201);
    }

    private long customer(String name) {
        return createCustomer(owner, business, name, null);
    }

    private long memberId(long userId) {
        for (JsonNode m : ok(get(owner.token(), B + "/team", business), 200)) {
            if (m.path("userId").asLong() == userId) {
                return m.path("id").asLong();
            }
        }
        throw new AssertionError("Not on the team: " + userId);
    }

    private void setSalary(Session person, String salary) {
        ok(patch(owner.token(), B + "/team/{m}", Map.of("monthlySalary", salary), business, memberId(person.userId())), 200);
    }

    private long entryFor(String name) {
        for (JsonNode e : ok(get(owner.token(), B + "/payroll", business), 200)) {
            if (name.equals(e.path("fullName").asString())) {
                return e.path("id").asLong();
            }
        }
        throw new AssertionError("No payroll entry for " + name);
    }

    private JsonNode entry(long id) {
        for (JsonNode e : ok(get(owner.token(), B + "/payroll", business), 200)) {
            if (e.path("id").asLong() == id) {
                return e;
            }
        }
        throw new AssertionError("No payroll entry " + id);
    }

    private static List<JsonNode> list(JsonNode array) {
        List<JsonNode> items = new ArrayList<>();
        array.forEach(items::add);
        return items;
    }
}
