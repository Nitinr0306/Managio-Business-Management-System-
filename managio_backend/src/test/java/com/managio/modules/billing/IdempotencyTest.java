package com.managio.modules.billing;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Money actions with an {@code Idempotency-Key} happen once, however often the request arrives: retries after a
 * lost response, double submits and concurrent duplicates (docs/API.md, "Retries").
 */
class IdempotencyTest extends IntegrationTest {

    private static final String INVOICES = "/api/v1/businesses/{b}/invoices";

    private Session owner;
    private long business;
    private long customer;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        enableAllModules(owner, business);
        customer = createCustomer(owner, business, "Rohan Mehta", "9845012873");
    }

    private long invoice(String price) {
        return ok(post(owner.token(), INVOICES, Map.of("customerId", customer,
                "lines", List.of(Map.of("description", "Personal training", "unitPrice", price, "quantity", 1))), business), 201)
                .path("id").asLong();
    }

    private MvcTestResult postWithKey(String key, String uri, Object body, Object... vars) {
        return mvc.post().uri(uri, vars).headers(h -> h.setBearerAuth(owner.token())).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)).exchange();
    }

    private long count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE business_id = :b").param("b", business)
                .query(Long.class).single();
    }

    @Test
    void aRetriedPartialPaymentIsRecordedOnce() {
        setUp();
        long invoice = invoice("2000");
        Map<String, Object> body = Map.of("amount", "500", "method", "UPI");

        JsonNode first = ok(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", body, business, invoice), 201);
        JsonNode retry = ok(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", body, business, invoice), 201);

        assertThat(retry).isEqualTo(first);
        assertThat(count("payments")).isEqualTo(1);
        assertThat(ok(get(owner.token(), INVOICES + "/{i}", business, invoice), 200).path("balance").decimalValue())
                .isEqualByComparingTo("1500");
        // A new key is a new payment.
        ok(postWithKey("pay-0b81d455", INVOICES + "/{i}/payments", body, business, invoice), 201);
        assertThat(count("payments")).isEqualTo(2);
    }

    @Test
    void aKeyCannotBeReusedForADifferentRequest() {
        setUp();
        long invoice = invoice("2000");
        ok(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", Map.of("amount", "500", "method", "UPI"), business, invoice), 201);

        problem(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", Map.of("amount", "700", "method", "UPI"), business, invoice),
                422, "idempotency.key_reused");
        long other = invoice("300");
        problem(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", Map.of("amount", "500", "method", "UPI"), business, other),
                422, "idempotency.key_reused");
        assertThat(count("payments")).isEqualTo(1);
    }

    @Test
    void aFailedRequestDoesNotUseUpItsKey() {
        setUp();
        long invoice = invoice("2000");
        problem(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", Map.of("amount", "5000", "method", "UPI"), business, invoice),
                400, "validation.failed");
        problem(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", Map.of("amount", "5000", "method", "UPI"), business, invoice),
                400, "validation.failed");

        ok(postWithKey("pay-7f3a9c21", INVOICES + "/{i}/payments", Map.of("amount", "2000", "method", "UPI"), business, invoice), 201);
        assertThat(count("payments")).isEqualTo(1);
    }

    @Test
    void concurrentDuplicatesCreateOneRecord() throws Exception {
        setUp();
        long invoice = invoice("2000");
        Map<String, Object> body = Map.of("amount", "500", "method", "CASH");
        int copies = 6;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MvcTestResult>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(copies)) {
            for (int i = 0; i < copies; i++) {
                Callable<MvcTestResult> send = () -> {
                    start.await();
                    return postWithKey("pay-concurrent-1", INVOICES + "/{i}/payments", body, business, invoice);
                };
                results.add(pool.submit(send));
            }
            start.countDown();
            List<String> receipts = new ArrayList<>();
            for (Future<MvcTestResult> result : results) {
                receipts.add(ok(result.get(), 201).path("receiptNumber").asString());
            }
            assertThat(receipts).hasSize(copies).containsOnly(receipts.getFirst());
        }
        assertThat(count("payments")).isEqualTo(1);
    }

    @Test
    void retriedSalesRefundsAndExpensesAreRecordedOnce() {
        setUp();
        Map<String, Object> sale = Map.of("invoice", Map.of("customerId", customer,
                        "lines", List.of(Map.of("description", "Protein bar", "unitPrice", "120", "quantity", 2))),
                "payment", Map.of("method", "CASH"));
        JsonNode sold = ok(postWithKey("sale-1a2b3c4d", "/api/v1/businesses/{b}/sales", sale, business), 201);
        assertThat(ok(postWithKey("sale-1a2b3c4d", "/api/v1/businesses/{b}/sales", sale, business), 201)).isEqualTo(sold);
        assertThat(count("invoices")).isEqualTo(1);

        long payment = sold.path("payment").path("id").asLong();
        Map<String, Object> refund = Map.of("amount", "40", "method", "CASH", "reason", "Bar was damaged");
        ok(postWithKey("refund-1a2b3c4d", "/api/v1/businesses/{b}/payments/{p}/refunds", refund, business, payment), 201);
        ok(postWithKey("refund-1a2b3c4d", "/api/v1/businesses/{b}/payments/{p}/refunds", refund, business, payment), 201);
        assertThat(jdbc.sql("SELECT count(*) FROM payments WHERE kind = 'REFUND'").query(Long.class).single()).isEqualTo(1);

        long category = ok(get(owner.token(), "/api/v1/businesses/{b}/expense-categories", business), 200).get(0).path("id").asLong();
        Map<String, Object> expense = Map.of("categoryId", category, "amount", "850", "method", "UPI", "description", "Cleaning");
        ok(postWithKey("expense-1a2b3c4d", "/api/v1/businesses/{b}/expenses", expense, business), 201);
        ok(postWithKey("expense-1a2b3c4d", "/api/v1/businesses/{b}/expenses", expense, business), 201);
        assertThat(count("expenses")).isEqualTo(1);
    }

    @Test
    void malformedKeysAreRefusedAndRequestsWithoutAKeyWorkAsBefore() {
        setUp();
        long invoice = invoice("2000");
        problem(postWithKey("short", INVOICES + "/{i}/payments", Map.of("amount", "100", "method", "UPI"), business, invoice),
                400, "idempotency.invalid_key");
        problem(postWithKey("has spaces in it", INVOICES + "/{i}/payments", Map.of("amount", "100", "method", "UPI"), business, invoice),
                400, "idempotency.invalid_key");
        ok(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "100", "method", "UPI"), business, invoice), 201);
        ok(post(owner.token(), INVOICES + "/{i}/payments", Map.of("amount", "100", "method", "UPI"), business, invoice), 201);
        assertThat(count("payments")).isEqualTo(2);
    }
}
