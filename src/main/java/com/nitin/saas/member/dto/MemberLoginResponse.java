package com.nitin.saas.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private MemberInfo member;

    private SubscriptionInfo activeSubscription;

    private BusinessInfo business;

    private LocalDateTime lastLoginAt;

    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private Long id;
        private Long businessId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String phone;
        private String email;
        private LocalDate dateOfBirth;
        private String gender;
        private String status;
        private LocalDateTime memberSince;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionInfo {
        private Long subscriptionId;
        private String planName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Integer daysRemaining;
        private BigDecimal amountPaid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BusinessInfo {
        private Long id;
        private String name;
        private String address;
        private String phone;
        private String email;
    }
}