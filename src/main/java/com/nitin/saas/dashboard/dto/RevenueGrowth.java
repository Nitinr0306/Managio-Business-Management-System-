package com.nitin.saas.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueGrowth {
    private BigDecimal thisMonth;
    private BigDecimal lastMonth;
    private Double growthPercentage;
    private List<MonthlyRevenuePoint> monthlyTrend;
}