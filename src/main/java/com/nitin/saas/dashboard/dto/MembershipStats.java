package com.nitin.saas.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipStats {
    private Integer totalSubscriptions;
    private Integer completedSubscriptions;
    private BigDecimal totalSpent;
    private LocalDate memberSince;
}
