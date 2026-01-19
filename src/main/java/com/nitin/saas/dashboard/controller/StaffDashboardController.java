package com.nitin.saas.dashboard.controller;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.security.UserPrincipal;
import com.nitin.saas.dashboard.dto.StaffDashboardResponse;
import com.nitin.saas.dashboard.service.StaffDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/staff")
public class StaffDashboardController {

    private final StaffDashboardService service;
    private final BusinessRepository businessRepository;

    public StaffDashboardController(StaffDashboardService service, BusinessRepository businessRepository) {
        this.service = service;
        this.businessRepository = businessRepository;

    }

    @GetMapping
    public StaffDashboardResponse dashboard(
            @PathVariable String businessCode,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Business business =
                businessRepository.findByCode(businessCode)
                        .orElseThrow(() -> new IllegalArgumentException("Business not found"));

        return service.getDashboard(business);
    }
}
