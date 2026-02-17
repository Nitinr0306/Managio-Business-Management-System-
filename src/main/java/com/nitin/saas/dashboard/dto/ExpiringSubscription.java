package com.nitin.saas.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiringSubscription {
    private Long memberId;
    private String memberName;
    private String planName;
    private LocalDate endDate;
    private Integer daysRemaining;
    private String phone;
    private String email;
}