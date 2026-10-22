package com.managio.modules.requests.repository;

import com.managio.modules.requests.domain.ApprovalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByIdAndBusinessId(long id, long businessId);

    /** Locked while deciding so two approvers can't both approve (and pay) the same request. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ApprovalRequest r where r.id = :id and r.businessId = :businessId")
    Optional<ApprovalRequest> findForUpdate(long id, long businessId);
}
