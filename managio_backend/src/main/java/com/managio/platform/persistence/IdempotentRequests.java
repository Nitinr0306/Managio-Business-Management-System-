package com.managio.platform.persistence;

import com.managio.platform.error.ApiException;
import com.managio.platform.web.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Runs an action that creates money records at most once per {@code Idempotency-Key} (docs/API.md, "Retries").
 *
 * <p>The key is claimed with an insert into {@code idempotency_keys} in the same transaction as the action. A second
 * request with the same key waits on that row until the first commits, then gets the first response back; if the
 * first failed, its transaction (and so its claim) rolled back and the retry runs normally. Reusing a key for a
 * different request is refused. Requests without a key behave as before.
 */
@Component
public class IdempotentRequests {

    public static final String HEADER = "Idempotency-Key";
    static final Duration KEEP_FOR = Duration.ofDays(7);
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final JsonMapper json;
    private final Clock clock;

    public IdempotentRequests(JdbcClient jdbc, PlatformTransactionManager transactionManager, JsonMapper json,
                              Clock clock) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.json = json;
        this.clock = clock;
    }

    /**
     * Runs {@code action} once for {@code key}, or returns the response it gave the first time.
     *
     * @param operation names the action and its target, e.g. {@code "payment:invoice:42"}
     * @param request   the request body; a retry must send the same one
     */
    public <T> T once(long businessId, long userId, @Nullable String key, String operation, Object request,
                      Class<T> responseType, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        if (!KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "idempotency.invalid_key",
                    "The Idempotency-Key header must be 8 to 100 letters, digits, dashes or underscores.");
        }
        String requestHash = sha256(operation + "\n" + json.writeValueAsString(request));
        return transactions.execute(status -> {
            int claimed = jdbc.sql("""
                            INSERT INTO idempotency_keys (business_id, user_id, idem_key, operation, request_hash, created_at)
                            VALUES (:businessId, :userId, :key, :operation, :hash, :now)
                            ON CONFLICT (business_id, user_id, idem_key) DO NOTHING""")
                    .param("businessId", businessId).param("userId", userId).param("key", key)
                    .param("operation", operation).param("hash", requestHash).param("now", Timestamp.from(clock.instant()))
                    .update();
            if (claimed == 0) {
                return replay(businessId, userId, key, operation, requestHash, responseType);
            }
            T response = action.get();
            jdbc.sql("UPDATE idempotency_keys SET response_body = :body "
                            + "WHERE business_id = :businessId AND user_id = :userId AND idem_key = :key")
                    .param("body", json.writeValueAsString(response))
                    .param("businessId", businessId).param("userId", userId).param("key", key)
                    .update();
            return response;
        });
    }

    /** Deletes keys older than a week; returns how many. */
    public int deleteOlderThanAWeek() {
        Instant cutoff = clock.instant().minus(KEEP_FOR);
        return jdbc.sql("DELETE FROM idempotency_keys WHERE created_at < :cutoff").param("cutoff", Timestamp.from(cutoff)).update();
    }

    private record Stored(String operation, String requestHash, @Nullable String responseBody) {
    }

    private <T> T replay(long businessId, long userId, String key, String operation, String requestHash,
                         Class<T> responseType) {
        Stored stored = jdbc.sql("""
                        SELECT operation, request_hash, response_body FROM idempotency_keys
                        WHERE business_id = :businessId AND user_id = :userId AND idem_key = :key""")
                .param("businessId", businessId).param("userId", userId).param("key", key)
                .query((rs, i) -> new Stored(rs.getString("operation"), rs.getString("request_hash"),
                        rs.getString("response_body")))
                .single();
        if (!stored.operation().equals(operation) || !stored.requestHash().equals(requestHash)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency.key_reused",
                    "This was already submitted with different details. Reload the page and try again.");
        }
        if (stored.responseBody() == null) {
            throw ApiException.conflict("idempotency.in_progress", "This is still being saved. Wait a moment and reload.");
        }
        return json.readValue(stored.responseBody(), responseType);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
