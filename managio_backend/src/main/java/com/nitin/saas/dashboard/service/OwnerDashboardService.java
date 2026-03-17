package com.nitin.saas.dashboard.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.dashboard.dto.*;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.dto.PaymentMethodStats;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.payment.service.PaymentStatsService;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerDashboardService {

    private final MemberRepository             memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository            paymentRepository;
    private final PaymentStatsService          paymentStatsService;
    private final BusinessService              businessService;

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getOwnerDashboard(Long businessId) {
        businessService.requireAccess(businessId);

        // ── Time anchors ───────────────────────────────────────────────────────
        LocalDateTime now          = LocalDateTime.now();
        LocalDateTime monthStart   = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayStart   = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tenYearsAgo  = now.minusYears(10);
        LocalDate     today        = LocalDate.now();

        // ── Member statistics ──────────────────────────────────────────────────
        Long totalMembers       = memberRepository.countActiveByBusinessId(businessId);
        Long activeMembers      = memberRepository.countActiveMembers(businessId);
        Long inactiveMembers    = totalMembers - activeMembers;

        // FIX CVL-007: real query instead of hardcoded 0
        Long newMembersThisMonth = memberRepository.countNewMembersSince(businessId, monthStart);

        // ── Subscription statistics ────────────────────────────────────────────
        Long activeSubscriptions = subscriptionRepository.countActiveByBusinessId(businessId);
        LocalDate today2         = LocalDate.now();

        List<MemberSubscription> expiring7  = subscriptionRepository
                .findExpiringByBusinessId(businessId, today2, today2.plusDays(7));
        List<MemberSubscription> expiring30 = subscriptionRepository
                .findExpiringByBusinessId(businessId, today2, today2.plusDays(30));

        Long expiringIn7Days  = (long) expiring7.size();
        Long expiringIn30Days = (long) expiring30.size();

        List<ExpiringSubscription> upcomingExpirations =
                buildExpiringSubscriptions(expiring7.stream().limit(10).collect(Collectors.toList()));

        // ── Revenue statistics ─────────────────────────────────────────────────
        BigDecimal totalRevenue   = nvl(paymentRepository.calculateRevenue(businessId, tenYearsAgo));
        BigDecimal monthlyRevenue = nvl(paymentRepository.calculateRevenue(businessId, monthStart));
        BigDecimal todayRevenue   = nvl(paymentRepository.calculateRevenue(businessId, todayStart));

        BigDecimal avgPerMember = totalMembers > 0
                ? totalRevenue.divide(new BigDecimal(totalMembers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Payment statistics ─────────────────────────────────────────────────
        Long totalPayments     = paymentRepository.countByBusinessId(businessId);
        Long paymentsThisMonth = paymentRepository.countByBusinessIdSince(businessId, monthStart);

        List<Payment> recentPaymentEntities = paymentRepository
                .findRecentPayments(businessId, now.minusDays(7))
                .stream().limit(10).collect(Collectors.toList());
        List<RecentPayment> recentPayments = buildRecentPayments(recentPaymentEntities);

        // ── Payment method stats ───────────────────────────────────────────────
        PaymentMethodStats paymentMethodStats = paymentStatsService.getPaymentMethodStats(businessId);

        // ── Growth metrics ─────────────────────────────────────────────────────
        MemberGrowth  memberGrowth  = buildMemberGrowth(businessId, monthStart, newMembersThisMonth);
        RevenueGrowth revenueGrowth = buildRevenueGrowth(businessId, monthStart, monthlyRevenue);

        return OwnerDashboardResponse.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .inactiveMembers(inactiveMembers)
                .newMembersThisMonth(newMembersThisMonth)
                .activeSubscriptions(activeSubscriptions)
                .expiringIn7Days(expiringIn7Days)
                .expiringIn30Days(expiringIn30Days)
                .upcomingExpirations(upcomingExpirations)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .todayRevenue(todayRevenue)
                .averageRevenuePerMember(avgPerMember)
                .totalPayments(totalPayments)
                .paymentsThisMonth(paymentsThisMonth)
                .recentPayments(recentPayments)
                .paymentMethodStats(paymentMethodStats)
                .memberGrowth(memberGrowth)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<ExpiringSubscription> buildExpiringSubscriptions(List<MemberSubscription> subs) {
        if (subs.isEmpty()) return Collections.emptyList();
        Set<Long> ids = subs.stream().map(MemberSubscription::getMemberId).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return subs.stream().map(sub -> {
            Member m = memberMap.get(sub.getMemberId());
            long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
            return ExpiringSubscription.builder()
                    .memberId(sub.getMemberId())
                    .memberName(m != null ? m.getFullName() : "Unknown")
                    .planName("Plan #" + sub.getPlanId())
                    .endDate(sub.getEndDate())
                    .daysRemaining((int) days)
                    .phone(m != null ? m.getPhone() : null)
                    .email(m != null ? m.getEmail() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<RecentPayment> buildRecentPayments(List<Payment> payments) {
        if (payments.isEmpty()) return Collections.emptyList();
        Set<Long> ids = payments.stream().map(Payment::getMemberId).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return payments.stream().map(p -> {
            Member m = memberMap.get(p.getMemberId());
            return RecentPayment.builder()
                    .paymentId(p.getId())
                    .memberName(m != null ? m.getFullName() : "Unknown")
                    .amount(p.getAmount())
                    .paymentMethod(p.getPaymentMethod().getDisplayName())
                    .paidAt(p.getCreatedAt().toLocalDate())
                    .build();
        }).collect(Collectors.toList());
    }

    private MemberGrowth buildMemberGrowth(Long businessId,
                                           LocalDateTime monthStart,
                                           Long thisMonth) {
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        Long lastMonth = memberRepository.countNewMembersSince(businessId, lastMonthStart)
                - thisMonth;
        if (lastMonth < 0) lastMonth = 0L;

        double growth = lastMonth > 0
                ? ((double)(thisMonth - lastMonth) / lastMonth) * 100.0
                : 0.0;

        return MemberGrowth.builder()
                .thisMonth(thisMonth)
                .lastMonth(lastMonth)
                .growthPercentage(growth)
                .monthlyTrend(Collections.emptyList())
                .build();
    }

    /**
     * FIX H1: last-month revenue = (revenue since lastMonthStart) - (revenue since thisMonthStart)
     * Previous code set lastMonth = totalSincLastMonthStart which included this month too,
     * making growth always ≤ 0.
     */
    private RevenueGrowth buildRevenueGrowth(Long businessId,
                                             LocalDateTime monthStart,
                                             BigDecimal thisMonthRevenue) {
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        BigDecimal sinceLastMonth = nvl(paymentRepository.calculateRevenue(businessId, lastMonthStart));
        BigDecimal lastMonthRevenue = sinceLastMonth.subtract(thisMonthRevenue);
        if (lastMonthRevenue.compareTo(BigDecimal.ZERO) < 0) lastMonthRevenue = BigDecimal.ZERO;

        double growthPct = lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0
                ? thisMonthRevenue.subtract(lastMonthRevenue)
                .divide(lastMonthRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue()
                : 0.0;

        return RevenueGrowth.builder()
                .thisMonth(thisMonthRevenue)
                .lastMonth(lastMonthRevenue)
                .growthPercentage(growthPct)
                .monthlyTrend(Collections.emptyList())
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}