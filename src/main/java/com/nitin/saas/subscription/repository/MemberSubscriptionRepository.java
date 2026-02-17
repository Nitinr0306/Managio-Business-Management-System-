package com.nitin.saas.subscription.repository;

import com.nitin.saas.subscription.entity.MemberSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {

  @Query("SELECT ms FROM MemberSubscription ms WHERE ms.memberId = :memberId AND ms.status = 'ACTIVE'")
  Optional<MemberSubscription> findActiveSubscriptionByMemberId(@Param("memberId") Long memberId);

  @Query("SELECT ms FROM MemberSubscription ms WHERE ms.memberId = :memberId ORDER BY ms.createdAt DESC")
  List<MemberSubscription> findByMemberId(@Param("memberId") Long memberId);

  @Query("SELECT ms FROM MemberSubscription ms WHERE ms.endDate < :date AND ms.status = 'ACTIVE'")
  List<MemberSubscription> findExpiredSubscriptions(@Param("date") LocalDate date);

  @Query("SELECT ms FROM MemberSubscription ms WHERE ms.endDate BETWEEN :start AND :end AND ms.status = 'ACTIVE'")
  List<MemberSubscription> findExpiringSubscriptions(@Param("start") LocalDate start, @Param("end") LocalDate end);

  @Query("SELECT COUNT(ms) FROM MemberSubscription ms JOIN Member m ON ms.memberId = m.id WHERE m.businessId = :businessId AND ms.status = 'ACTIVE'")
  Long countActiveByBusinessId(@Param("businessId") Long businessId);
}