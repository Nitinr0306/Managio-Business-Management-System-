package com.nitin.saas.payment.repository;

import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.enums.PaymentMethod;
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

    @Query("SELECT p FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByBusinessId(
            @Param("businessId") Long businessId,
            Pageable pageable);

    @Query("SELECT p FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId AND p.paymentMethod = :paymentMethod " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByBusinessIdAndPaymentMethod(
            @Param("businessId") Long businessId,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            Pageable pageable);

    @Query("SELECT p FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId AND p.paymentMethod IN :paymentMethods " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByBusinessIdAndPaymentMethods(
            @Param("businessId") Long businessId,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) " +
            "FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId AND p.createdAt >= :since")
    BigDecimal calculateRevenue(
            @Param("businessId") Long businessId,
            @Param("since")      LocalDateTime since);

    @Query("SELECT p FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId AND p.createdAt >= :since " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findRecentPayments(
            @Param("businessId") Long businessId,
            @Param("since")      LocalDateTime since);

    @Query("SELECT p FROM Payment p " +
            "WHERE p.createdAt >= :startDate AND p.createdAt < :endDate")
    List<Payment> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate);

    @Query("SELECT COUNT(p) " +
            "FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId")
    Long countByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COUNT(p) " +
            "FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId AND p.createdAt >= :since")
    Long countByBusinessIdSince(
            @Param("businessId") Long businessId,
            @Param("since")      LocalDateTime since);

    /**
     * FIX M4: returns one row per payment method with its count and total amount.
     * Each Object[] element is: [PaymentMethod enum, Long count, BigDecimal totalAmount].
     *
     * Replaces the previous approach of loading ALL payments into a List<Payment>
     * in the JVM and then grouping in memory — which caused OOM for large businesses.
     * This single SQL query returns a tiny result set regardless of payment volume.
     */
    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) " +
            "FROM Payment p " +
            "JOIN Member m ON p.memberId = m.id " +
            "WHERE m.businessId = :businessId " +
            "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentStatsGroupedByMethod(@Param("businessId") Long businessId);
}