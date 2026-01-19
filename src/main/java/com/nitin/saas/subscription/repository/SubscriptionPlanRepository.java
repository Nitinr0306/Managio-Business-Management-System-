package com.nitin.saas.subscription.repository;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByBusinessAndCode(
            Business business,
            String code
    );
}
