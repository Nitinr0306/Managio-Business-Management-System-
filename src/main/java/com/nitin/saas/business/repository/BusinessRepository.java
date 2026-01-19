package com.nitin.saas.business.repository;

import com.nitin.saas.business.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    boolean existsBySlug(String slug);
    Optional<Business> findByCode(String code);

}
