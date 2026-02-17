package com.nitin.saas.payment.repository;

import com.nitin.saas.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMemberId(Long memberId);

    Page<Payment> findByMemberId(Long memberId, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN Member m ON p.memberId = m.id WHERE m.businessId = :businessId ORDER BY p.createdAt DESC")
    Page<Payment> findByBusinessId(@Param("businessId") Long businessId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p JOIN Member m ON p.memberId = m.id WHERE m.businessId = :businessId AND p.createdAt >= :since")
    BigDecimal calculateRevenue(@Param("businessId") Long businessId, @Param("since") LocalDateTime since);

    @Query("SELECT p FROM Payment p JOIN Member m ON p.memberId = m.id WHERE m.businessId = :businessId AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPayments(@Param("businessId") Long businessId, @Param("since") LocalDateTime since);

    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt < :endDate")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}