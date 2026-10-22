package com.managio.modules.billing.repository;

import com.managio.modules.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndBusinessId(long id, long businessId);

    List<Payment> findByInvoiceIdOrderByIdAsc(long invoiceId);

    /** Money in minus money returned, ignoring voided records (BIL-6). */
    @Query("select coalesce(sum(case when p.kind = com.managio.modules.billing.domain.Payment.Kind.PAYMENT then p.amount "
            + "else -p.amount end), 0) from Payment p where p.invoiceId = :invoiceId and p.voidedAt is null")
    BigDecimal netPaid(long invoiceId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.refundOfId = :paymentId and p.voidedAt is null")
    BigDecimal refundedAmount(long paymentId);
}
