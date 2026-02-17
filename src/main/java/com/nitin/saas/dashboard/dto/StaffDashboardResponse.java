
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
public class StaffDashboardResponse {
    private Long membersAddedToday;
    private Long totalActiveMembers;
    private List<ExpiringSubscription> expiringThisWeek;
    private List<RecentPayment> recentPayments;
    private List<TaskReminder> taskReminders;
}