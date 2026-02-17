package com.nitin.saas.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberGrowth {
    private Long thisMonth;
    private Long lastMonth;
    private Double growthPercentage;
    private List<MonthlyGrowthPoint> monthlyTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyGrowthPoint {
        private String month;
        private Long count;
    }
}