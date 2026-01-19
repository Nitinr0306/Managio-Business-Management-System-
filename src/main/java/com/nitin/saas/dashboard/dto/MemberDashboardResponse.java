package com.nitin.saas.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public class MemberDashboardResponse {

    public String planName;
    public String subscriptionStatus;
    public LocalDate startDate;
    public LocalDate endDate;

    public List<MemberPaymentItem> payments;
}
