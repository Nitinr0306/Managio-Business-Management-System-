package com.nitin.saas.business.service;

import com.nitin.saas.business.dto.BusinessStatisticsResponse;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.staff.repository.StaffRepository;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessStatisticsService {

    private final MemberRepository             memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository            paymentRepository;
    private final BusinessService              businessService;
    private final StaffRepository              staffRepository;

    @Transactional(readOnly = true)
    public BusinessStatisticsResponse getStatistics(Long businessId) {
        businessService.requireAccess(businessId);

        // ── Time anchors ───────────────────────────────────────────────────────
        LocalDateTime now         = LocalDateTime.now();
        LocalDateTime monthStart  = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayStart  = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tenYearsAgo = now.minusYears(10);
        LocalDate     today       = LocalDate.now();

        // ── Member statistics ──────────────────────────────────────────────────
        Long totalMembers         = memberRepository.countActiveByBusinessId(businessId);
        Long activeMembers        = memberRepository.countActiveMembers(businessId);
        Long inactiveMembers      = totalMembers - activeMembers;

        // FIX CVL-007: now uses real query instead of hardcoded 0
        Long newMembersThisMonth  = memberRepository.countNewMembersSince(businessId, monthStart);

        // ── Subscription statistics ────────────────────────────────────────────
        Long activeSubscriptions  = subscriptionRepository.countActiveByBusinessId(businessId);

        Long expiringIn7Days      = subscriptionRepository
                .countExpiringByBusinessId(businessId, today, today.plusDays(7));
        Long expiringIn30Days     = subscriptionRepository
                .countExpiringByBusinessId(businessId, today, today.plusDays(30));

        // FIX CVL-008: count subscriptions whose endDate is in the past with status ACTIVE
        // (the nightly job may not have processed them yet, so we count them from DB)
        Long expiredSubscriptions = subscriptionRepository
                .countExpiredByBusinessId(businessId, today);

        // ── Revenue statistics ─────────────────────────────────────────────────
        BigDecimal totalRevenue   = nvl(paymentRepository.calculateRevenue(businessId, tenYearsAgo));
        BigDecimal monthlyRevenue = nvl(paymentRepository.calculateRevenue(businessId, monthStart));
        BigDecimal todayRevenue   = nvl(paymentRepository.calculateRevenue(businessId, todayStart));

        BigDecimal avgRevenuePerMember = totalMembers > 0
                ? totalRevenue.divide(new BigDecimal(totalMembers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Payment statistics ─────────────────────────────────────────────────
        Long totalPayments        = paymentRepository.countByBusinessId(businessId);
        Long paymentsThisMonth    = paymentRepository.countByBusinessIdSince(businessId, monthStart);

        // ── Staff statistics ───────────────────────────────────────────────────
        Long totalStaff           = staffRepository.countActiveStaff(businessId);

        return BusinessStatisticsResponse.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .inactiveMembers(inactiveMembers)
                .newMembersThisMonth(newMembersThisMonth)
                .activeSubscriptions(activeSubscriptions)
                .expiredSubscriptions(expiredSubscriptions)
                .expiringIn7Days(expiringIn7Days)
                .expiringIn30Days(expiringIn30Days)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .todayRevenue(todayRevenue)
                .averageRevenuePerMember(avgRevenuePerMember)
                .totalPayments(totalPayments)
                .paymentsThisMonth(paymentsThisMonth)
                .totalStaff(totalStaff)
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}