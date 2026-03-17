package com.nitin.saas.dashboard.controller;

import com.nitin.saas.dashboard.dto.MemberDashboardResponse;
import com.nitin.saas.dashboard.service.MemberDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/{memberId}/dashboard")
@RequiredArgsConstructor
@Tag(name = "Member Dashboard", description = "Member dashboard endpoints")
public class MemberDashboardController {

    private final MemberDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get member dashboard")
    public ResponseEntity<MemberDashboardResponse> getMemberDashboard(@PathVariable Long memberId) {
        MemberDashboardResponse response = dashboardService.getMemberDashboard(memberId);
        return ResponseEntity.ok(response);
    }
}