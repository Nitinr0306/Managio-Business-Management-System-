package com.managio.modules.customers.repository;

import com.managio.modules.customers.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndBusinessId(long id, long businessId);

    @Query("select c from Customer c where c.businessId = :businessId and c.phone = :phone and c.status = 'ACTIVE' "
            + "and (:excludeId is null or c.id <> :excludeId)")
    List<Customer> findActiveByPhone(long businessId, String phone, Long excludeId);

    @Query("select c from Customer c where c.businessId = :businessId and c.portalEnabled = true "
            + "and (lower(c.email) = :identifier or c.phone = :identifier)")
    Optional<Customer> findPortalAccount(long businessId, String identifier);

    @Query("select count(c) > 0 from Customer c where c.businessId = :businessId and c.portalEnabled = true "
            + "and c.id <> :excludeId and (lower(c.email) = :email or c.phone = :phone)")
    boolean portalLoginTaken(long businessId, String email, String phone, long excludeId);
}
