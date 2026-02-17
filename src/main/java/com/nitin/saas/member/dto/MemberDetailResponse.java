package com.nitin.saas.member.dto;

import com.nitin.saas.payment.dto.PaymentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDetailResponse {
    // Basic Info
    private Long id;
    private Long businessId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Active Subscription Details
    private ActiveSubscriptionInfo activeSubscription;

    // Payment History
    private List<PaymentResponse> paymentHistory;

    // Statistics
    private BigDecimal totalPaid;
    private Integer totalSubscriptions;
    private LocalDateTime memberSince;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveSubscriptionInfo {
        private Long subscriptionId;
        private Long planId;
        private String planName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Integer daysRemaining;
        private BigDecimal amount;
    }
}