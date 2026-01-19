package com.nitin.saas.subscription.repository;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository
        extends JpaRepository<MemberSubscription, Long> {

    boolean existsByMemberAndStatus(
            Member member,
            SubscriptionStatus status
    );
    List<MemberSubscription> findAllByStatusAndEndDateBefore(
            SubscriptionStatus status,
            LocalDate date
    );
    @Query("""
        SELECT COUNT(s)
        FROM MemberSubscription s
        WHERE s.member.business = :business
          AND s.status = :status
    """)
    long countByBusinessAndStatus(
            Business business,
            SubscriptionStatus status
    );

    @Query("""
        SELECT COUNT(s)
        FROM MemberSubscription s
        WHERE s.member.business = :business
          AND s.status = 'ACTIVE'
          AND s.endDate <= :date
    """)
    long countExpiringSoon(
            Business business,
            LocalDate date
    );

    @Query("""
SELECT s.plan.name, COALESCE(SUM(p.amount),0)
FROM Payment p
JOIN p.subscription s
WHERE p.business = :business
AND p.status = 'SUCCESS'
GROUP BY s.plan.name
""")
    List<Object[]> revenueByPlan(Business business);

    Optional<MemberSubscription> findByMemberAndStatus(
            Member member,
            SubscriptionStatus status
    );
}
