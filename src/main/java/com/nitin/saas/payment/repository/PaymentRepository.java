package com.nitin.saas.payment.repository;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.dashboard.dto.DailyRevenuePoint;
import com.nitin.saas.dashboard.dto.MemberPaymentItem;
import com.nitin.saas.dashboard.dto.RecentPaymentItem;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);



    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.business = :business
        AND p.status = 'SUCCESS'
        AND MONTH(p.createdAt) = MONTH(CURRENT_DATE)
        AND YEAR(p.createdAt) = YEAR(CURRENT_DATE)
    """)
    long sumMonthlyRevenue(Business business);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.business = :business
        AND p.status = 'SUCCESS'
        AND DATE(p.createdAt) = CURRENT_DATE
    """)
    long sumTodayRevenue(Business business);

    @Query("""
SELECT new com.nitin.saas.dashboard.dto.DailyRevenuePoint(
    CAST(p.createdAt AS date),
    COALESCE(SUM(p.amount), 0)
)
FROM Payment p
WHERE p.business = :business
  AND p.status = 'SUCCESS'
  AND p.createdAt >= :fromDate
GROUP BY CAST(p.createdAt AS date)
ORDER BY CAST(p.createdAt AS date)
""")
    List<DailyRevenuePoint> dailyRevenue(
            Business business,
            LocalDateTime fromDate
    );


    @Query("""
SELECT p.provider, COALESCE(SUM(p.amount),0)
FROM Payment p
WHERE p.business = :business
AND p.status = 'SUCCESS'
GROUP BY p.provider
""")
    List<Object[]> revenueByProvider(Business business);

    long countByBusinessAndStatus(Business business, PaymentStatus status);

    long countByBusinessAndStatusAndCreatedAtAfter(
            Business business,
            PaymentStatus status,
            LocalDateTime startOfDay
    );

    @Query("""
SELECT new com.nitin.saas.dashboard.dto.RecentPaymentItem(
    p.id,
    p.subscription.member.name,
    p.amount,
    p.status,
    p.createdAt
)
FROM Payment p
WHERE p.business = :business
ORDER BY p.createdAt DESC
""")
    List<RecentPaymentItem> findRecentPayments(
            Business business,
            Pageable pageable
    );

    @Query("""
SELECT new com.nitin.saas.dashboard.dto.MemberPaymentItem(
    p.amount,
    p.status,
    p.provider,
    p.createdAt
)
FROM Payment p
WHERE p.subscription.member = :member
ORDER BY p.createdAt DESC
""")
    List<MemberPaymentItem> findPaymentsForMember(
            Member member
    );





}

