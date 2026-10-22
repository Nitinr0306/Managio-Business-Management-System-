package com.managio.modules.payroll.repository;

import com.managio.modules.payroll.domain.PayrollEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PayrollEntryRepository extends JpaRepository<PayrollEntry, Long> {

    /** Locked so two people paying the same salary can't both pay the full remainder. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayrollEntry p where p.id = :id and p.businessId = :businessId")
    Optional<PayrollEntry> findForUpdate(long id, long businessId);
}
