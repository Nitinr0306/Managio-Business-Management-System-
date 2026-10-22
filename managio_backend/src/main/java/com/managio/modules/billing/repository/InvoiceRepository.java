package com.managio.modules.billing.repository;

import com.managio.modules.billing.domain.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByIdAndBusinessId(long id, long businessId);

    /** Serialises payments, refunds and voids on one invoice so the balance can't be overdrawn. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id and i.businessId = :businessId")
    Optional<Invoice> findForUpdate(long id, long businessId);

    List<Invoice> findByBusinessIdAndMembershipIdOrderByIdAsc(long businessId, long membershipId);
}
