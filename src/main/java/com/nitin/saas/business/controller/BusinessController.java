package com.nitin.saas.business.controller;

import com.nitin.saas.business.dto.BusinessResponse;
import com.nitin.saas.business.dto.BusinessStatisticsResponse;
import com.nitin.saas.business.dto.CreateBusinessRequest;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.business.service.BusinessStatisticsService;
import com.nitin.saas.staff.dto.StaffResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nitin.saas.staff.service.StaffService;  // ADD THIS
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
@Tag(name = "Business", description = "Business management")
public class BusinessController {

        private final BusinessService businessService;
        private final BusinessStatisticsService statisticsService;
        private final StaffService staffService;  // >>> ADD THIS FIELD <<<

        @PostMapping
        @Operation(summary = "Create business")
        public ResponseEntity<BusinessResponse> createBusiness(
                @Valid @RequestBody CreateBusinessRequest request) {
                BusinessResponse response = businessService.createBusiness(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/my")
        @Operation(summary = "Get my businesses")
        public ResponseEntity<List<BusinessResponse>> getMyBusinesses() {
                List<BusinessResponse> response = businessService.getMyBusinesses();
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get business by ID")
        public ResponseEntity<BusinessResponse> getBusinessById(@PathVariable Long id) {
                BusinessResponse response = businessService.getBusinessById(id);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}/statistics")
        @Operation(summary = "Get business statistics")
        public ResponseEntity<BusinessStatisticsResponse> getStatistics(@PathVariable Long id) {
                BusinessStatisticsResponse response = statisticsService.getStatistics(id);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update business")
        public ResponseEntity<BusinessResponse> updateBusiness(
                @PathVariable Long id,
                @Valid @RequestBody CreateBusinessRequest request) {
                BusinessResponse response = businessService.updateBusiness(id, request);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete business")
        public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
                businessService.deleteBusiness(id);
                return ResponseEntity.noContent().build();
        }

        // ADD THIS METHOD TO BUSINESS CONTROLLER (OPTIONAL)

        @GetMapping("/{id}/staff")
        @Operation(summary = "Get all staff for business")
        public ResponseEntity<List<StaffResponse>> getBusinessStaff(@PathVariable Long id) {
                List<StaffResponse> response = staffService.getBusinessStaffList(id);
                return ResponseEntity.ok(response);
        }

}