package com.nitin.saas.business.repository;

import com.nitin.saas.business.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    boolean existsBySlug(String slug);
}
