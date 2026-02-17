package com.nitin.saas.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignSubscriptionRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Member ID is required")
    private Long memberId;
}