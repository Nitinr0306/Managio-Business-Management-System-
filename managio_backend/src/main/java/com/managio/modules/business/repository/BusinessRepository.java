package com.managio.modules.business.repository;

import com.managio.modules.business.domain.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    boolean existsByCode(String code);

    Optional<Business> findByCodeAndArchivedAtIsNull(String code);
}
