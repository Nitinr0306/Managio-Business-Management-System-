package com.nitin.saas.business.service;

import com.nitin.saas.business.dto.BusinessStatisticsResponse;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.repository.PaymentRepository;
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

    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public BusinessStatisticsResponse getStatistics(Long businessId) {
        businessService.requireAccess(businessId);

        // Member statistics
        Long totalMembers = memberRepository.countActiveByBusinessId(businessId);
        Long activeMembers = memberRepository.countActiveMembers(businessId);
        Long inactiveMembers = totalMembers - activeMembers;

        // New members this month
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        // Simplified - would need proper query for this
        Long newMembersThisMonth = 0L;

        // Subscription statistics
        Long activeSubscriptions = subscriptionRepository.countActiveByBusinessId(businessId);
        Long expiredSubscriptions = 0L; // Would need proper query

        LocalDate today = LocalDate.now();
        Long expiringIn7Days = subscriptionRepository
                .findExpiringSubscriptions(today, today.plusDays(7)).stream().count();
        Long expiringIn30Days = subscriptionRepository
                .findExpiringSubscriptions(today, today.plusDays(30)).stream().count();

        // Revenue statistics
        BigDecimal totalRevenue = paymentRepository.calculateRevenue(businessId,
                LocalDateTime.now().minusYears(10));
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal monthlyRevenue = paymentRepository.calculateRevenue(businessId, monthStart);
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        BigDecimal todayRevenue = paymentRepository.calculateRevenue(businessId, todayStart);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        BigDecimal averageRevenuePerMember = totalMembers > 0
                ? totalRevenue.divide(new BigDecimal(totalMembers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Payment statistics
        Long totalPayments = paymentRepository.findRecentPayments(businessId,
                LocalDateTime.now().minusYears(10)).stream().count();
        Long paymentsThisMonth = paymentRepository.findRecentPayments(businessId, monthStart).stream().count();

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
                .averageRevenuePerMember(averageRevenuePerMember)
                .totalPayments(totalPayments)
                .paymentsThisMonth(paymentsThisMonth)
                .build();
    }
}