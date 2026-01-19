package com.nitin.saas.dashboard.dto;

import java.util.List;

public class StaffDashboardResponse {

    public long membersAddedToday;
    public long activeMembers;
    public long expiringSubscriptionsNext7Days;

    public long successfulPaymentsToday;
    public long failedPaymentsToday;

    public List<RecentPaymentItem> recentPayments;
}
