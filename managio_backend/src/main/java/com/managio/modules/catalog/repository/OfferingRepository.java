package com.managio.modules.catalog.repository;

import com.managio.modules.catalog.domain.Offering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface OfferingRepository extends JpaRepository<Offering, Long> {

    Optional<Offering> findByIdAndBusinessId(long id, long businessId);

    /** Locks the row so concurrent sales can't both take the last unit of stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offering o where o.id = :id and o.businessId = :businessId")
    Optional<Offering> findForUpdate(long id, long businessId);

    @Query("select o from Offering o where o.businessId = :businessId and (:kind is null or o.kind = :kind) "
            + "and (:includeInactive = true or o.active = true) order by o.kind, lower(o.name)")
    List<Offering> list(long businessId, Offering.Kind kind, boolean includeInactive);
}
