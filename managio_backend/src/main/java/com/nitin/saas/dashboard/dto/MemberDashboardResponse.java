package com.nitin.saas.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDashboardResponse {
   private String memberName;
   private String planName;
   private LocalDate subscriptionEndDate;
   private Integer daysRemaining;
   private String status;
   private List<PaymentHistory> paymentHistory;
   private BigDecimal totalPaid;
   private MembershipStats membershipStats;
}
