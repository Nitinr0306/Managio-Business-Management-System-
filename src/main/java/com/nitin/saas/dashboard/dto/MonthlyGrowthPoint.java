package com.nitin.saas.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyGrowthPoint {
    private String month;
    private Long count;
}