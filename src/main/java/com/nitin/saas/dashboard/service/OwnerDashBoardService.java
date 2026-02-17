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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerDashboardService {

    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatsService paymentStatsService;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getOwnerDashboard(Long businessId) {
        businessService.requireAccess(businessId);

        // Member Statistics
        Long totalMembers = memberRepository.countActiveByBusinessId(businessId);
        Long activeMembers = memberRepository.countActiveMembers(businessId);
        Long inactiveMembers = totalMembers - activeMembers;
        Long newMembersThisMonth = 0L; // Simplified

        // Subscription Statistics
        Long activeSubscriptions = subscriptionRepository.countActiveByBusinessId(businessId);

        LocalDate today = LocalDate.now();
        List<MemberSubscription> expiring7Days = subscriptionRepository
                .findExpiringSubscriptions(today, today.plusDays(7));
        List<MemberSubscription> expiring30Days = subscriptionRepository
                .findExpiringSubscriptions(today, today.plusDays(30));

        Long expiringIn7Days = (long) expiring7Days.size();
        Long expiringIn30Days = (long) expiring30Days.size();

        List<ExpiringSubscription> upcomingExpirations = expiring7Days.stream()
                .limit(10)
                .map(this::mapToExpiringSubscription)
                .collect(Collectors.toList());

        // Revenue Statistics
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        BigDecimal totalRevenue = paymentRepository.calculateRevenue(businessId,
                LocalDateTime.now().minusYears(10));
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal monthlyRevenue = paymentRepository.calculateRevenue(businessId, monthStart);
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;

        BigDecimal todayRevenue = paymentRepository.calculateRevenue(businessId, todayStart);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        BigDecimal averageRevenuePerMember = totalMembers > 0
                ? totalRevenue.divide(new BigDecimal(totalMembers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Payment Statistics
        List<Payment> allPayments = paymentRepository.findRecentPayments(businessId,
                LocalDateTime.now().minusYears(10));
        Long totalPayments = (long) allPayments.size();

        List<Payment> monthPayments = paymentRepository.findRecentPayments(businessId, monthStart);
        Long paymentsThisMonth = (long) monthPayments.size();

        List<RecentPayment> recentPayments = paymentRepository
                .findRecentPayments(businessId, LocalDateTime.now().minusDays(7))
                .stream()
                .limit(10)
                .map(this::mapToRecentPayment)
                .collect(Collectors.toList());

        // Payment Method Stats
        PaymentMethodStats paymentMethodStats = paymentStatsService.getPaymentMethodStats(businessId);

        // Growth Metrics
        MemberGrowth memberGrowth = calculateMemberGrowth(businessId, monthStart);
        RevenueGrowth revenueGrowth = calculateRevenueGrowth(businessId, monthStart, monthlyRevenue);

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
                .averageRevenuePerMember(averageRevenuePerMember)
                .totalPayments(totalPayments)
                .paymentsThisMonth(paymentsThisMonth)
                .recentPayments(recentPayments)
                .paymentMethodStats(paymentMethodStats)
                .memberGrowth(memberGrowth)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    private ExpiringSubscription mapToExpiringSubscription(MemberSubscription sub) {
        Member member = memberRepository.findById(sub.getMemberId()).orElse(null);
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());

        return ExpiringSubscription.builder()
                .memberId(sub.getMemberId())
                .memberName(member != null ? member.getFullName() : "Unknown")
                .planName("Plan #" + sub.getPlanId())
                .endDate(sub.getEndDate())
                .daysRemaining((int) daysRemaining)
                .phone(member != null ? member.getPhone() : null)
                .email(member != null ? member.getEmail() : null)
                .build();
    }

    private RecentPayment mapToRecentPayment(Payment payment) {
        Member member = memberRepository.findById(payment.getMemberId()).orElse(null);

        return RecentPayment.builder()
                .paymentId(payment.getId())
                .memberName(member != null ? member.getFullName() : "Unknown")
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().getDisplayName())
                .paidAt(payment.getCreatedAt().toLocalDate())
                .build();
    }

    private MemberGrowth calculateMemberGrowth(Long businessId, LocalDateTime monthStart) {
        Long thisMonth = memberRepository.countActiveByBusinessId(businessId);
        Long lastMonth = thisMonth; // Simplified - would need historical data

        Double growthPercentage = lastMonth > 0
                ? ((thisMonth - lastMonth) * 100.0) / lastMonth
                : 0.0;

        return MemberGrowth.builder()
                .thisMonth(thisMonth)
                .lastMonth(lastMonth)
                .growthPercentage(growthPercentage)
                .monthlyTrend(new ArrayList<>())
                .build();
    }

    private RevenueGrowth calculateRevenueGrowth(Long businessId, LocalDateTime monthStart,
                                                 BigDecimal thisMonthRevenue) {
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        BigDecimal lastMonthRevenue = paymentRepository.calculateRevenue(businessId, lastMonthStart);
        if (lastMonthRevenue == null) lastMonthRevenue = BigDecimal.ZERO;

        Double growthPercentage = lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0
                ? thisMonthRevenue.subtract(lastMonthRevenue)
                .divide(lastMonthRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue()
                : 0.0;

        return RevenueGrowth.builder()
                .thisMonth(thisMonthRevenue)
                .lastMonth(lastMonthRevenue)
                .growthPercentage(growthPercentage)
                .monthlyTrend(new ArrayList<>())
                .build();
    }
}