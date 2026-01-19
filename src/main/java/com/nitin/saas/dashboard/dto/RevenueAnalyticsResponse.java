package com.nitin.saas.dashboard.dto;

import java.util.List;
import java.util.Map;

public class RevenueAnalyticsResponse {

    public List<DailyRevenuePoint> dailyRevenueLast30Days;

    public Map<String, Long> revenueByPlan;

    public Map<String, Long> revenueByPaymentProvider;

    public long successfulPayments;
    public long failedPayments;
}
