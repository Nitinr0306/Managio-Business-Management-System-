package com.nitin.saas.dashboard.dto;

import com.nitin.saas.payment.dto.PaymentMethodStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Owner Dashboard Response
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerDashboardResponse {
    // Member Statistics
    private Long totalMembers;
    private Long activeMembers;
    private Long inactiveMembers;
    private Long newMembersThisMonth;

    // Subscription Statistics
    private Long activeSubscriptions;
    private Long expiringIn7Days;
    private Long expiringIn30Days;
    private List<ExpiringSubscription> upcomingExpirations;

    // Revenue Statistics
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal averageRevenuePerMember;

    // Payment Statistics
    private Long totalPayments;
    private Long paymentsThisMonth;
    private List<RecentPayment> recentPayments;

    // Payment Method Breakdown
    private PaymentMethodStats paymentMethodStats;

    // Growth Metrics
    private MemberGrowth memberGrowth;
    private RevenueGrowth revenueGrowth;
}
