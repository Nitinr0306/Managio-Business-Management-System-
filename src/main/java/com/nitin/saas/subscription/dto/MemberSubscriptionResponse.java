package com.nitin.saas.subscription.dto;

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
class MemberSubscriptionResponse {
    private Long id;
    private Long memberId;
    private Long planId;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private BigDecimal amount;
    private Integer daysRemaining;
    private LocalDateTime createdAt;
}