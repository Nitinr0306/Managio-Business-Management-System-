package com.managio.modules.billing.repository;

import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.domain.InvoiceState;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.billing.dto.BillingDtos.CustomerRef;
import com.managio.modules.billing.dto.BillingDtos.InvoiceRow;
import com.managio.modules.billing.dto.BillingDtos.PaymentResponse;
import com.managio.modules.billing.domain.DocumentPrefixes;
import com.managio.platform.persistence.SqlText;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** List read models for invoices and payments; one query per page, never one per row. */
@Component
@RequiredArgsConstructor
public class BillingQueries {

    /** Which invoices a list shows (BIL-2 states). */
    public enum InvoiceView {
        ALL("TRUE"),
        OPEN("i.status = 'ISSUED' AND i.total > coalesce(p.paid, 0)"),
        OVERDUE("i.status = 'ISSUED' AND i.total > coalesce(p.paid, 0) AND i.due_on < :today"),
        PAID("i.status = 'ISSUED' AND coalesce(p.paid, 0) >= i.total"),
        VOID("i.status = 'VOID'"),
        /** Everything a customer should see: void invoices are left out. */
        NOT_VOID("i.status = 'ISSUED'");

        private final String condition;

        InvoiceView(String condition) {
            this.condition = condition;
        }
    }

    private static final String PAID_CTE = """
            WITH p AS (
                SELECT invoice_id, sum(CASE kind WHEN 'PAYMENT' THEN amount ELSE -amount END) AS paid
                FROM payments WHERE business_id = :businessId AND voided_at IS NULL GROUP BY invoice_id)
            """;

    private final JdbcClient jdbc;

    public DocumentPrefixes prefixes(long businessId) {
        return jdbc.sql("SELECT invoice_prefix, receipt_prefix FROM businesses WHERE id = :id").param("id", businessId)
                .query((rs, i) -> new DocumentPrefixes(rs.getString("invoice_prefix"), rs.getString("receipt_prefix"))).single();
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceRow> invoices(long businessId, LocalDate today, InvoiceView view, Long customerId, String q,
                                             int page, int size) {
        String where = " WHERE i.business_id = :businessId AND " + view.condition
                + (customerId == null ? "" : " AND i.customer_id = :customerId")
                + (q == null || q.isBlank() ? "" : " AND (CAST(i.number AS text) = :term OR lower(c.full_name) LIKE :like)");
        String from = " FROM invoices i LEFT JOIN p ON p.invoice_id = i.id LEFT JOIN customers c ON c.id = i.customer_id";
        DocumentPrefixes prefixes = prefixes(businessId);

        long total = bind(jdbc.sql(PAID_CTE + "SELECT count(*)" + from + where), businessId, today, customerId, q)
                .query(Long.class).single();
        List<InvoiceRow> rows = bind(jdbc.sql(PAID_CTE + """
                        SELECT i.id, i.number, i.customer_id, c.number AS customer_number, c.full_name, i.membership_id,
                               i.issued_on, i.due_on, i.total, coalesce(p.paid, 0) AS paid, i.status
                        """ + from + where + " ORDER BY i.due_on DESC, i.id DESC LIMIT :limit OFFSET :offset"),
                businessId, today, customerId, q)
                .param("limit", size).param("offset", page * size)
                .query((rs, i) -> invoiceRow(rs, prefixes, today)).list();
        return PageResponse.of(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> payments(long businessId, LocalDate from, LocalDate to, PaymentMethod method,
                                                  Long customerId, int page, int size) {
        return payments(businessId, from, to, method, customerId, true, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> payments(long businessId, LocalDate from, LocalDate to, PaymentMethod method,
                                                  Long customerId, boolean includeVoided, int page, int size) {
        String where = " WHERE p.business_id = :businessId" + (includeVoided ? "" : " AND p.voided_at IS NULL")
                + (from == null ? "" : " AND p.paid_on >= :from") + (to == null ? "" : " AND p.paid_on <= :to")
                + (method == null ? "" : " AND p.method = :method")
                + (customerId == null ? "" : " AND p.customer_id = :customerId");
        DocumentPrefixes prefixes = prefixes(businessId);
        long total = bindPayments(jdbc.sql("SELECT count(*) FROM payments p" + where), businessId, from, to, method, customerId)
                .query(Long.class).single();
        List<PaymentResponse> rows = bindPayments(jdbc.sql("""
                        SELECT p.id, p.number, p.kind, p.invoice_id, i.number AS invoice_number, p.customer_id,
                               c.number AS customer_number, c.full_name AS customer_name, p.amount, p.method, p.reference,
                               p.paid_on, p.received_at, u.full_name AS received_by, p.notes, p.refund_of_id,
                               p.voided_at, p.void_reason
                        FROM payments p
                        JOIN invoices i ON i.id = p.invoice_id
                        LEFT JOIN customers c ON c.id = p.customer_id
                        LEFT JOIN users u ON u.id = p.received_by_user_id""" + where
                        + " ORDER BY p.received_at DESC, p.id DESC LIMIT :limit OFFSET :offset"),
                businessId, from, to, method, customerId)
                .param("limit", size).param("offset", page * size)
                .query((rs, i) -> paymentRow(rs, prefixes)).list();
        return PageResponse.of(rows, page, size, total);
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, long businessId, LocalDate today,
                                                 Long customerId, String q) {
        spec = spec.param("businessId", businessId).param("today", today);
        if (customerId != null) {
            spec = spec.param("customerId", customerId);
        }
        if (q != null && !q.isBlank()) {
            String term = q.trim().toLowerCase();
            spec = spec.param("term", term.replaceAll("[^0-9]", "").isEmpty() ? "-1" : term.replaceAll("[^0-9]", ""))
                    .param("like", SqlText.containing(term));
        }
        return spec;
    }

    private static JdbcClient.StatementSpec bindPayments(JdbcClient.StatementSpec spec, long businessId, LocalDate from,
                                                         LocalDate to, PaymentMethod method, Long customerId) {
        spec = spec.param("businessId", businessId);
        if (from != null) {
            spec = spec.param("from", from);
        }
        if (to != null) {
            spec = spec.param("to", to);
        }
        if (method != null) {
            spec = spec.param("method", method.name());
        }
        if (customerId != null) {
            spec = spec.param("customerId", customerId);
        }
        return spec;
    }

    private static InvoiceRow invoiceRow(ResultSet rs, DocumentPrefixes prefixes, LocalDate today) throws SQLException {
        BigDecimal total = rs.getBigDecimal("total");
        BigDecimal paid = rs.getBigDecimal("paid");
        Invoice.Status status = Invoice.Status.valueOf(rs.getString("status"));
        LocalDate dueOn = rs.getObject("due_on", LocalDate.class);
        BigDecimal balance = status == Invoice.Status.VOID ? BigDecimal.ZERO.setScale(2) : total.subtract(paid).max(BigDecimal.ZERO);
        return new InvoiceRow(rs.getLong("id"), rs.getInt("number"), prefixes.invoiceNumber(rs.getInt("number")),
                customer(rs, "customer_id", "customer_number", "full_name"), (Long) rs.getObject("membership_id"),
                rs.getObject("issued_on", LocalDate.class), dueOn, total, paid, balance,
                InvoiceState.of(status, total, paid, dueOn, today));
    }

    private static PaymentResponse paymentRow(ResultSet rs, DocumentPrefixes prefixes) throws SQLException {
        return new PaymentResponse(rs.getLong("id"), rs.getInt("number"), prefixes.receiptNumber(rs.getInt("number")),
                rs.getString("kind"), rs.getLong("invoice_id"), prefixes.invoiceNumber(rs.getInt("invoice_number")),
                customer(rs, "customer_id", "customer_number", "customer_name"), rs.getBigDecimal("amount"),
                PaymentMethod.valueOf(rs.getString("method")), rs.getString("reference"),
                rs.getObject("paid_on", LocalDate.class), rs.getTimestamp("received_at").toInstant(),
                rs.getString("received_by"), rs.getString("notes"), (Long) rs.getObject("refund_of_id"),
                rs.getTimestamp("voided_at") != null, rs.getString("void_reason"));
    }

    private static CustomerRef customer(ResultSet rs, String idColumn, String numberColumn, String nameColumn)
            throws SQLException {
        Long id = (Long) rs.getObject(idColumn);
        return id == null ? null : new CustomerRef(id, rs.getInt(numberColumn), rs.getString(nameColumn));
    }
}
