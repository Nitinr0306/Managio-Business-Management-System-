package com.nitin.saas.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionPlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive
    private BigDecimal price;

    @NotNull(message = "Duration is required")
    @Positive
    private Integer durationDays;
}