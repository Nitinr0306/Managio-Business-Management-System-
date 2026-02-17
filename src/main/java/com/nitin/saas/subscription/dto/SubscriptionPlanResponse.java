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
public class SubscriptionPlanResponse {
    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

