package com.nitin.saas.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessStatisticsResponse {
    // Member Statistics
    private Long totalMembers;
    private Long activeMembers;
    private Long inactiveMembers;
    private Long newMembersThisMonth;

    // Subscription Statistics
    private Long activeSubscriptions;
    private Long expiredSubscriptions;
    private Long expiringIn7Days;
    private Long expiringIn30Days;

    // Revenue Statistics
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal averageRevenuePerMember;

    // Payment Statistics
    private Long totalPayments;
    private Long paymentsThisMonth;
}