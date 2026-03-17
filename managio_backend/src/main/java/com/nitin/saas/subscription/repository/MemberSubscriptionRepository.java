package com.nitin.saas.subscription.repository;

import com.nitin.saas.subscription.entity.MemberSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {

  // ── Single member ─────────────────────────────────────────────────────────

  @Query("SELECT ms FROM MemberSubscription ms " +
          "WHERE ms.memberId = :memberId AND ms.status = 'ACTIVE'")
  Optional<MemberSubscription> findActiveSubscriptionByMemberId(@Param("memberId") Long memberId);

  @Query("SELECT ms FROM MemberSubscription ms " +
          "WHERE ms.memberId = :memberId ORDER BY ms.createdAt DESC")
  List<MemberSubscription> findByMemberId(@Param("memberId") Long memberId);

  // ── Business subscriptions list (paginated) ───────────────────────────────

  @Query("SELECT ms FROM MemberSubscription ms " +
          "JOIN Member m ON ms.memberId = m.id " +
          "WHERE m.businessId = :businessId " +
          "ORDER BY ms.createdAt DESC")
  Page<MemberSubscription> findByBusinessId(
          @Param("businessId") Long businessId,
          Pageable pageable);

  // ── Batch active lookup (FIX: eliminates N+1 in getMembersWithSubscriptions) ──

  /**
   * Returns the ACTIVE subscription for each member in the provided ID set.
   * One query instead of N queries (one per member).
   *
   * Usage: load the result into a Map&lt;Long, MemberSubscription&gt; keyed by memberId.
   */
  @Query("SELECT ms FROM MemberSubscription ms " +
          "WHERE ms.memberId IN :memberIds AND ms.status = 'ACTIVE'")
  List<MemberSubscription> findActiveSubscriptionsByMemberIds(
          @Param("memberIds") java.util.Collection<Long> memberIds);

  // ── Expiry management ─────────────────────────────────────────────────────

  @Query("SELECT ms FROM MemberSubscription ms " +
          "WHERE ms.endDate < :date AND ms.status = 'ACTIVE'")
  List<MemberSubscription> findExpiredSubscriptions(@Param("date") LocalDate date);

  /**
   * Global expiry window query — used ONLY by the ExpiryReminderScheduler.
   * For business-scoped queries use {@link #findExpiringByBusinessId}.
   */
  @Query("SELECT ms FROM MemberSubscription ms " +
          "WHERE ms.endDate BETWEEN :start AND :end AND ms.status = 'ACTIVE'")
  List<MemberSubscription> findExpiringSubscriptions(
          @Param("start") LocalDate start,
          @Param("end")   LocalDate end);

  // ── Business-scoped variants ──────────────────────────────────────────────

  @Query("SELECT ms FROM MemberSubscription ms " +
          "JOIN Member m ON ms.memberId = m.id " +
          "WHERE m.businessId = :businessId " +
          "  AND ms.endDate BETWEEN :start AND :end " +
          "  AND ms.status = 'ACTIVE' " +
          "ORDER BY ms.endDate ASC")
  List<MemberSubscription> findExpiringByBusinessId(
          @Param("businessId") Long businessId,
          @Param("start")      LocalDate start,
          @Param("end")        LocalDate end);

  @Query("SELECT COUNT(ms) FROM MemberSubscription ms " +
          "JOIN Member m ON ms.memberId = m.id " +
          "WHERE m.businessId = :businessId " +
          "  AND ms.endDate BETWEEN :start AND :end " +
          "  AND ms.status = 'ACTIVE'")
  Long countExpiringByBusinessId(
          @Param("businessId") Long businessId,
          @Param("start")      LocalDate start,
          @Param("end")        LocalDate end);

  @Query("SELECT COUNT(ms) FROM MemberSubscription ms " +
          "JOIN Member m ON ms.memberId = m.id " +
          "WHERE m.businessId = :businessId AND ms.status = 'ACTIVE'")
  Long countActiveByBusinessId(@Param("businessId") Long businessId);

  /**
   * FIX CVL-008: counts subscriptions that are EXPIRED or past end-date
   * (nightly job may not have processed them yet).
   */
  @Query("SELECT COUNT(ms) FROM MemberSubscription ms " +
          "JOIN Member m ON ms.memberId = m.id " +
          "WHERE m.businessId = :businessId " +
          "  AND (ms.status = 'EXPIRED' OR (ms.status = 'ACTIVE' AND ms.endDate < :today))")
  Long countExpiredByBusinessId(
          @Param("businessId") Long businessId,
          @Param("today")      LocalDate today);
}