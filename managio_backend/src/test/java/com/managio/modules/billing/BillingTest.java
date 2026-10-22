package com.managio.modules.billing;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Invoices, payments, refunds and sales: BIL-1 to BIL-10. */
class BillingTest extends IntegrationTest {

    private static final String INVOICES = "/api/v1/businesses/{b}/invoices";

    private Session owner;
    private Session frontDesk;
    private long business;
    private long rohan;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        enableAllModules(owner, business);
        frontDesk = joinAs(owner, business, roleId(owner, business, "FRONT_DESK"), "Divya", "divya@example.com");
        rohan = createCustomer(owner, business, "Rohan Mehta", "9845012873");
    }

    private JsonNode invoice(Session session, Object customerId, List<Map<String, Object>> lines) {
        Map<String, Object> body = new java.util.HashMap<>();
        if (customerId != null) {
            body.put("customerId", customerId);
        }
        body.put("lines", lines);
        return ok(post(session.token(), INVOICES, body, business), 201);
    }

    private static Map<String, Object> custom(String description, String price, int quantity) {
        return Map.of("description", description, "unitPrice", price, "quantity", quantity);
    }

    private JsonNode pay(Session session, long invoiceId, String amount, String method) {
        return ok(post(session.token(), INVOICES + "/{i}/payments", Map.of("amount", amount, "method", method), business, invoiceId), 201);
    }

    private JsonNode fetch(long invoiceId) {
        return ok(get(owner.token(), INVOICES + "/{i}", business, invoiceId), 200);
    }

    @Test
    void reversedDateRangesAreRejectedInsteadOfReturningNothing() {
        setUp();
        problem(get(owner.token(), "/api/v1/businesses/{b}/payments?from=2026-10-20&to=2026-10-01", business), 400, "validation.failed");
        problem(get(owner.token(), "/api/v1/businesses/{b}/expenses?from=2026-10-20&to=2026-10-01", business), 400, "validation.failed");
        problem(get(owner.token(), "/api/v1/businesses/{b}/expenses/summary?from=2026-10-20&to=2026-10-01", business), 400,
                "validation.failed");
        ok(get(owner.token(), "/api/v1/businesses/{b}/payments?from=2026-10-01&to=2026-10-01", business), 200);
    }

    @Test
    void anInvoiceTotalsItsLinesDiscountAndTax() {
        setUp();
        long towel = ok(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PRODUCT", "name", "Towel",
                "price", "200", "taxRate", "18"), business), 201).path("id").asLong();

        JsonNode invoice = ok(post(owner.token(), INVOICES, Map.of("customerId", rohan, "discount", "100",
                "lines", List.of(Map.of("offeringId", towel, "quantity", 2), custom("Locker fee", "600", 1))), business), 201);

        // 400 + 600 = 1000; the tax is 18% of the towels only, on 90% of their value after the discount.
        assertThat(invoice.path("subtotal").decimalValue()).isEqualByComparingTo("1000");
        assertThat(invoice.path("tax").decimalValue()).isEqualByComparingTo("64.80");
        assertThat(invoice.path("total").decimalValue()).isEqualByComparingTo("964.80");
        assertThat(invoice.path("lines")).hasSize(2);
        assertThat(invoice.path("lines").get(0).path("description").asString()).isEqualTo("Towel");
        assertThat(invoice.path("state").asString()).isEqualTo("DUE");
        assertThat(invoice.path("balance").decimalValue()).isEqualByComparingTo("964.80");
    }

    @Test
    void paymentsCannotExceedTheBalanceAndReceiptsAreNumbered() {
        setUp();
        long id = invoice(owner, rohan, List.of(custom("Personal training", "1000", 1))).path("id").asLong();

        problem(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "1000.01", "method", "CASH"), business, id),
                400, "validation.failed");
        problem(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "0", "method", "CASH"), business, id), 400, "validation.failed");
        problem(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "10", "method", "CASH", "paidOn", "2026-10-21"),
                business, id), 400, "validation.failed");

        JsonNode first = pay(owner, id, "400", "CASH");
        JsonNode second = pay(owner, id, "600", "UPI");
        assertThat(first.path("receiptNumber").asString()).isEqualTo("RCT-0001");
        assertThat(second.path("receiptNumber").asString()).isEqualTo("RCT-0002");
        assertThat(fetch(id).path("state").asString()).isEqualTo("PAID");
        problem(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "1", "method", "CASH"), business, id),
                400, "validation.failed");
    }

    @Test
    void anInvoiceMovesFromDueToPartlyPaidToOverdue() {
        setUp();
        long id = invoice(owner, rohan, List.of(custom("Fee", "1000", 1))).path("id").asLong();
        assertThat(fetch(id).path("state").asString()).isEqualTo("DUE");

        pay(owner, id, "300", "CASH");
        assertThat(fetch(id).path("state").asString()).isEqualTo("PARTLY_PAID");
        assertThat(fetch(id).path("balance").decimalValue()).isEqualByComparingTo("700");

        clock.advance(Duration.ofDays(1));
        owner = reauth(owner);
        assertThat(fetch(id).path("state").asString()).isEqualTo("OVERDUE");
        assertThat(names(ok(get(owner.token(), INVOICES + "?view=OVERDUE", business), 200), "invoiceNumber")).containsExactly("INV-0001");
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/customers/{c}", business, rohan), 200)
                .path("balanceDue").decimalValue()).isEqualByComparingTo("700");
    }

    @Test
    void salesRecordTheInvoiceAndPaymentTogetherEvenForWalkIns() {
        setUp();
        Map<String, Object> sale = Map.of(
                "invoice", Map.of("lines", List.of(custom("Water bottle", "50", 2))),
                "payment", Map.of("method", "CASH"));

        JsonNode result = ok(post(frontDesk.token(), "/api/v1/businesses/{b}/sales", sale, business), 201);

        assertThat(result.path("invoice").path("state").asString()).isEqualTo("PAID");
        assertThat(result.path("invoice").path("customer").isNull()).isTrue();
        assertThat(result.path("payment").path("amount").decimalValue()).isEqualByComparingTo("100");
        // A walk-in can't leave something owing: nobody would be on the hook for it.
        problem(post(frontDesk.token(), "/api/v1/businesses/{b}/sales", Map.of(
                "invoice", Map.of("lines", List.of(custom("Water bottle", "50", 2))),
                "payment", Map.of("method", "CASH", "amount", "40")), business), 400, "validation.failed");
        JsonNode partial = ok(post(frontDesk.token(), "/api/v1/businesses/{b}/sales", Map.of(
                "invoice", Map.of("customerId", rohan, "lines", List.of(custom("Water bottle", "50", 2))),
                "payment", Map.of("method", "CASH", "amount", "40")), business), 201);
        assertThat(partial.path("invoice").path("balance").decimalValue()).isEqualByComparingTo("60");
    }

    @Test
    void discountsAndPriceChangesNeedThePermissionAndPlansNeedMemberships() {
        setUp();
        long towel = ok(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PRODUCT", "name", "Towel", "price", "200"),
                business), 201).path("id").asLong();
        long plan = createPlan(owner, business, "Monthly", "2499", 30);

        // BIL-4: front desk can sell but can't discount or change a catalogue price.
        problem(post(frontDesk.token(), INVOICES, Map.of("customerId", rohan, "discount", "10",
                "lines", List.of(custom("Fee", "100", 1))), business), 403, "auth.forbidden");
        problem(post(frontDesk.token(), INVOICES, Map.of("customerId", rohan,
                "lines", List.of(Map.of("offeringId", towel, "unitPrice", "150"))), business), 403, "auth.forbidden");
        ok(post(frontDesk.token(), INVOICES, Map.of("customerId", rohan,
                "lines", List.of(Map.of("offeringId", towel, "unitPrice", "200"))), business), 201);
        ok(post(owner.token(), INVOICES, Map.of("customerId", rohan,
                "lines", List.of(Map.of("offeringId", towel, "unitPrice", "150"))), business), 201);
        // BIL-9: plans are sold as memberships so they always have dates.
        problem(post(owner.token(), INVOICES, Map.of("customerId", rohan, "lines", List.of(Map.of("offeringId", plan))), business),
                422, "invoice.plan_line");
        problem(post(owner.token(), INVOICES, Map.of("customerId", rohan, "discount", "500",
                "lines", List.of(custom("Fee", "100", 1))), business), 400, "validation.failed");
        problem(post(owner.token(), INVOICES, Map.of("lines", List.of(custom("Fee", "100", 1))), business), 400, "validation.failed");
    }

    @Test
    void voidingNeedsThePermissionAReasonAndNoMoneyLeft() {
        setUp();
        long id = invoice(owner, rohan, List.of(custom("Fee", "1000", 1))).path("id").asLong();
        long paymentId = pay(owner, id, "1000", "CASH").path("id").asLong();

        problem(post(frontDesk.token(), INVOICES + "/{i}/void", Map.of("reason", "Mistake"), business, id), 403, "auth.forbidden");
        problem(post(owner.token(), INVOICES + "/{i}/void", Map.of("reason", "Mistake"), business, id), 422, "invoice.has_payments");

        problem(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/void", Map.of("reason", " "), business, paymentId), 400, "validation.failed");
        JsonNode voidedPayment = ok(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/void",
                Map.of("reason", "Entered twice"), business, paymentId), 200);
        assertThat(voidedPayment.path("voided").asBoolean()).isTrue();
        assertThat(fetch(id).path("balance").decimalValue()).isEqualByComparingTo("1000");
        problem(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/void", Map.of("reason", "Again"), business, paymentId),
                422, "payment.already_void");

        JsonNode voided = ok(post(owner.token(), INVOICES + "/{i}/void", Map.of("reason", "Mistake"), business, id), 200);
        assertThat(voided.path("state").asString()).isEqualTo("VOID");
        assertThat(voided.path("balance").decimalValue()).isEqualByComparingTo("0");
        problem(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "10", "method", "CASH"), business, id), 422, "invoice.void");
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/customers/{c}", business, rohan), 200)
                .path("balanceDue").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void refundsReduceWhatWasPaidAndCannotExceedThePayment() {
        setUp();
        long id = invoice(owner, rohan, List.of(custom("Fee", "1000", 1))).path("id").asLong();
        long paymentId = pay(owner, id, "1000", "CASH").path("id").asLong();

        problem(post(frontDesk.token(), "/api/v1/businesses/{b}/payments/{p}/refunds", Map.of("amount", "100", "method", "CASH",
                "reason", "Goodwill"), business, paymentId), 403, "auth.forbidden");
        JsonNode refund = ok(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/refunds", Map.of("amount", "300",
                "method", "CASH", "reason", "Left early"), business, paymentId), 201);
        assertThat(refund.path("kind").asString()).isEqualTo("REFUND");
        assertThat(refund.path("refundOfId").asLong()).isEqualTo(paymentId);
        assertThat(fetch(id).path("paid").decimalValue()).isEqualByComparingTo("700");
        assertThat(fetch(id).path("state").asString()).isEqualTo("PARTLY_PAID");

        problem(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/refunds", Map.of("amount", "700.01", "method", "CASH",
                "reason", "Too much"), business, paymentId), 400, "validation.failed");
        problem(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/void", Map.of("reason", "Wrong"), business, paymentId),
                422, "payment.has_refunds");
        // Voiding the refund puts the money back.
        ok(post(owner.token(), "/api/v1/businesses/{b}/payments/{p}/void", Map.of("reason", "Refund cancelled"), business,
                refund.path("id").asLong()), 200);
        assertThat(fetch(id).path("paid").decimalValue()).isEqualByComparingTo("1000");
    }

    @Test
    void stockFollowsSalesAndIsRestoredWhenAnInvoiceIsVoided() {
        setUp();
        long product = ok(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PRODUCT", "name", "Protein bar",
                "price", "120", "trackStock", true, "initialStock", 5), business), 201).path("id").asLong();

        long sold = invoice(owner, rohan, List.of(Map.of("offeringId", product, "quantity", 3))).path("id").asLong();
        assertThat(stock(product)).isEqualTo(2);
        problem(post(owner.token(), INVOICES, Map.of("customerId", rohan, "lines", List.of(Map.of("offeringId", product, "quantity", 3))),
                business), 422, "stock.insufficient");
        assertThat(stock(product)).isEqualTo(2);
        problem(post(owner.token(), INVOICES, Map.of("customerId", rohan,
                "lines", List.of(Map.of("offeringId", product, "quantity", "0.5"))), business), 400, "validation.failed");

        ok(post(owner.token(), INVOICES + "/{i}/void", Map.of("reason", "Customer changed mind"), business, sold), 200);
        assertThat(stock(product)).isEqualTo(5);
        ok(post(owner.token(), "/api/v1/businesses/{b}/offerings/{o}/stock", Map.of("change", -2, "reason", "Damaged"), business, product), 200);
        assertThat(stock(product)).isEqualTo(3);
    }

    @Test
    void soldItemsCanOnlyBeDeactivatedAndInactiveItemsAreHiddenFromFrontDesk() {
        setUp();
        long towel = ok(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "PRODUCT", "name", "Towel", "price", "200"),
                business), 201).path("id").asLong();
        long unused = ok(post(owner.token(), "/api/v1/businesses/{id}/offerings", Map.of("kind", "SERVICE", "name", "Massage", "price", "900"),
                business), 201).path("id").asLong();
        invoice(owner, rohan, List.of(Map.of("offeringId", towel)));

        problem(delete(owner.token(), "/api/v1/businesses/{b}/offerings/{o}", business, towel), 422, "offering.in_use");
        assertThat(delete(owner.token(), "/api/v1/businesses/{b}/offerings/{o}", business, unused)).hasStatus(204);

        ok(patch(owner.token(), "/api/v1/businesses/{b}/offerings/{o}", Map.of("kind", "PRODUCT", "name", "Towel", "price", "200",
                "active", false), business, towel), 200);
        assertThat(ok(get(frontDesk.token(), "/api/v1/businesses/{b}/offerings?includeInactive=true", business), 200)).isEmpty();
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/offerings?includeInactive=true", business), 200)).hasSize(1);
        problem(post(frontDesk.token(), "/api/v1/businesses/{b}/offerings", Map.of("kind", "PRODUCT", "name", "X", "price", "1"), business),
                403, "auth.forbidden");
        problem(post(owner.token(), INVOICES, Map.of("customerId", rohan, "lines", List.of(Map.of("offeringId", towel))), business),
                422, "offering.inactive");
    }

    @Test
    void invoicesAndPaymentsStayInsideTheirBusiness() {
        setUp();
        long id = invoice(owner, rohan, List.of(custom("Fee", "1000", 1))).path("id").asLong();
        long paymentId = pay(owner, id, "500", "CASH").path("id").asLong();
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");

        problem(get(rahul.token(), INVOICES + "/{i}", mart, id), 404, "not_found");
        problem(post(rahul.token(), INVOICES + "/{i}/payments", Map.of("amount", "10", "method", "CASH"), mart, id), 404, "not_found");
        problem(post(rahul.token(), INVOICES + "/{i}/void", Map.of("reason", "x"), mart, id), 404, "not_found");
        problem(post(rahul.token(), "/api/v1/businesses/{b}/payments/{p}/void", Map.of("reason", "x"), mart, paymentId), 404, "not_found");
        assertThat(ok(get(rahul.token(), "/api/v1/businesses/{b}/payments", mart), 200).path("items")).isEmpty();
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/payments", business), 200).path("items")).hasSize(1);
    }

    @Test
    void listsFilterAndSearch() {
        setUp();
        long priya = createCustomer(owner, business, "Priya Nair", null);
        long a = invoice(owner, rohan, List.of(custom("Fee", "100", 1))).path("id").asLong();
        long b = invoice(owner, priya, List.of(custom("Fee", "200", 1))).path("id").asLong();
        pay(owner, b, "200", "UPI");
        pay(owner, a, "50", "CASH");

        assertThat(names(ok(get(owner.token(), INVOICES + "?view=PAID", business), 200), "invoiceNumber")).containsExactly("INV-0002");
        assertThat(names(ok(get(owner.token(), INVOICES + "?view=OPEN", business), 200), "invoiceNumber")).containsExactly("INV-0001");
        assertThat(names(ok(get(owner.token(), INVOICES + "?q=priya", business), 200), "invoiceNumber")).containsExactly("INV-0002");
        assertThat(names(ok(get(owner.token(), INVOICES + "?q=1", business), 200), "invoiceNumber")).containsExactly("INV-0001");
        assertThat(names(ok(get(owner.token(), "/api/v1/businesses/{b}/payments?method=UPI", business), 200), "receiptNumber"))
                .containsExactly("RCT-0001");
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/payments?from=2026-10-21", business), 200).path("totalItems").asInt()).isZero();
        assertThat(ok(get(owner.token(), "/api/v1/businesses/{b}/payments?customerId={c}", business, rohan), 200).path("totalItems").asInt()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private int stock(long productId) {
        return ok(get(owner.token(), "/api/v1/businesses/{b}/offerings/{o}", business, productId), 200).path("stockQuantity").asInt();
    }

    private static List<String> names(JsonNode page, String field) {
        List<String> values = new ArrayList<>();
        page.path("items").forEach(i -> values.add(i.path(field).asString()));
        return values;
    }
}
