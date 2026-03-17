package com.nitin.saas.dashboard.controller;

import com.nitin.saas.dashboard.dto.StaffDashboardResponse;
import com.nitin.saas.dashboard.service.StaffDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/dashboard/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Dashboard", description = "Staff dashboard endpoints")
public class StaffDashboardController {

    private final StaffDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get staff dashboard")
    public ResponseEntity<StaffDashboardResponse> getStaffDashboard(@PathVariable Long businessId) {
        StaffDashboardResponse response = dashboardService.getStaffDashboard(businessId);
        return ResponseEntity.ok(response);
    }
}