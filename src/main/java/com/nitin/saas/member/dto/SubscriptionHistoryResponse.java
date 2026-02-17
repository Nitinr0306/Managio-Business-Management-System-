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
public class SubscriptionHistoryResponse {
    private Long id;
    private Long planId;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private BigDecimal amount;
    private Integer durationDays;
    private LocalDateTime createdAt;
}