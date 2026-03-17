package com.nitin.saas.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberListItemResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String status;

    // Subscription Info
    private String subscriptionStatus;  // "ACTIVE", "EXPIRED", "NONE"
    private String activePlanName;
    private LocalDate subscriptionEndDate;
    private Integer daysRemaining;
}