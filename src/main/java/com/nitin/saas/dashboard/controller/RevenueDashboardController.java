package com.nitin.saas.dashboard.controller;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.security.UserPrincipal;
import com.nitin.saas.dashboard.dto.RevenueAnalyticsResponse;
import com.nitin.saas.dashboard.service.RevenueAnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/revenue")
public class RevenueDashboardController {

    private final RevenueAnalyticsService service;
    private final BusinessRepository businessRepository;

    public RevenueDashboardController(RevenueAnalyticsService service, BusinessRepository businessRepository) {
        this.service = service;
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public RevenueAnalyticsResponse revenueDashboard(
            @PathVariable String businessCode,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Business business = businessRepository.findByCode(businessCode)
                .orElseThrow(() -> new IllegalArgumentException("Business not found"));
        return service.getRevenueAnalytics(business);
    }
}