package com.nitin.saas.subscription.repository;

import com.nitin.saas.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

        @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.businessId = :businessId AND sp.isActive = true")
        List<SubscriptionPlan> findActiveByBusinessId(@Param("businessId") Long businessId);
}