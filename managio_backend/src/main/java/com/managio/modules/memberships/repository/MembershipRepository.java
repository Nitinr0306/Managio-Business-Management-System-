package com.managio.modules.memberships.repository;

import com.managio.modules.memberships.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByIdAndBusinessId(long id, long businessId);

    List<Membership> findByBusinessIdAndCustomerIdOrderByEndsOnDescIdDesc(long businessId, long customerId);

    /** MEM-5: an overlapping non-cancelled membership of the same plan for the same customer. */
    @Query("select count(m) > 0 from Membership m where m.businessId = :businessId and m.customerId = :customerId "
            + "and m.offeringId = :offeringId and m.status = com.managio.modules.memberships.domain.Membership.Status.ACTIVE "
            + "and m.startsOn <= :endsOn and m.endsOn >= :startsOn")
    boolean overlaps(long businessId, long customerId, long offeringId, LocalDate startsOn, LocalDate endsOn);

    @Query("select count(m) > 0 from Membership m where m.renewedFromId = :id "
            + "and m.status = com.managio.modules.memberships.domain.Membership.Status.ACTIVE")
    boolean hasRenewal(long id);
}
