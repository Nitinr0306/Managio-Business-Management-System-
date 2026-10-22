package com.managio.modules.billing.service;

import com.managio.modules.billing.domain.DocumentPrefixes;
import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.domain.InvoiceState;
import com.managio.modules.billing.domain.Payment;
import com.managio.modules.billing.repository.BillingQueries;
import com.managio.modules.billing.repository.PaymentRepository;
import com.managio.modules.billing.dto.BillingDtos.CustomerRef;
import com.managio.modules.billing.dto.BillingDtos.InvoiceResponse;
import com.managio.modules.billing.dto.BillingDtos.LineResponse;
import com.managio.modules.billing.dto.BillingDtos.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Builds invoice and payment responses with their display numbers, customers and users. */
@Component
@RequiredArgsConstructor
class BillingViews {

    private final PaymentRepository payments;
    private final BillingQueries queries;
    private final JdbcClient jdbc;

    public DocumentPrefixes prefixes(long businessId) {
        return queries.prefixes(businessId);
    }

    CustomerRef customer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return jdbc.sql("SELECT id, number, full_name FROM customers WHERE id = :id").param("id", customerId)
                .query((rs, i) -> new CustomerRef(rs.getLong("id"), rs.getInt("number"), rs.getString("full_name")))
                .optional().orElse(null);
    }

    InvoiceResponse invoice(Invoice invoice, LocalDate today) {
        DocumentPrefixes prefixes = prefixes(invoice.getBusinessId());
        BigDecimal paid = payments.netPaid(invoice.getId());
        BigDecimal balance = invoice.isVoid() ? BigDecimal.ZERO.setScale(2) : invoice.getTotal().subtract(paid).max(BigDecimal.ZERO);
        List<LineResponse> lines = invoice.getLines().stream()
                .map(l -> new LineResponse(l.getOfferingId(), l.getDescription(), l.getQuantity(), l.getUnitPrice(),
                        l.getTaxRate(), l.getAmount()))
                .toList();
        List<PaymentResponse> history = payments.findByInvoiceIdOrderByIdAsc(invoice.getId()).stream()
                .map(p -> payment(p, prefixes, prefixes.invoiceNumber(invoice.getNumber())))
                .toList();
        return new InvoiceResponse(invoice.getId(), invoice.getNumber(), prefixes.invoiceNumber(invoice.getNumber()),
                customer(invoice.getCustomerId()), invoice.getMembershipId(), invoice.getIssuedOn(), invoice.getDueOn(),
                invoice.getSubtotal(), invoice.getDiscount(), invoice.getTax(), invoice.getTotal(), paid, balance,
                InvoiceState.of(invoice.getStatus(), invoice.getTotal(), paid, invoice.getDueOn(), today),
                invoice.getNotes(), invoice.getVoidReason(), lines, history);
    }

    PaymentResponse payment(Payment payment, DocumentPrefixes prefixes, String invoiceNumber) {
        String receivedBy = payment.getReceivedByUserId() == null ? null
                : jdbc.sql("SELECT full_name FROM users WHERE id = :id").param("id", payment.getReceivedByUserId())
                .query(String.class).optional().orElse(null);
        return new PaymentResponse(payment.getId(), payment.getNumber(), prefixes.receiptNumber(payment.getNumber()),
                payment.getKind().name(), payment.getInvoiceId(), invoiceNumber, customer(payment.getCustomerId()),
                payment.getAmount(), payment.getMethod(), payment.getReference(), payment.getPaidOn(),
                payment.getReceivedAt(), receivedBy, payment.getNotes(), payment.getRefundOfId(), payment.isVoid(),
                payment.getVoidReason());
    }
}
