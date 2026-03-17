package com.nitin.saas.dashboard.controller;

import com.nitin.saas.dashboard.dto.OwnerDashboardResponse;
import com.nitin.saas.dashboard.service.OwnerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/dashboard/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Dashboard", description = "Owner dashboard endpoints")
public class OwnerDashboardController {

    private final OwnerDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get owner dashboard")
    public ResponseEntity<OwnerDashboardResponse> getOwnerDashboard(@PathVariable Long businessId) {
        OwnerDashboardResponse response = dashboardService.getOwnerDashboard(businessId);
        return ResponseEntity.ok(response);
    }
}