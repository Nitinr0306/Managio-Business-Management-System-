package com.nitin.saas.business.controller;

import com.nitin.saas.business.dto.BusinessResponse;
import com.nitin.saas.business.dto.BusinessStatisticsResponse;
import com.nitin.saas.business.dto.CreateBusinessRequest;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.business.service.BusinessStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
@Tag(name = "Business", description = "Business management")
public class BusinessController {

        private final BusinessService           businessService;
        private final BusinessStatisticsService statisticsService;

        @PostMapping
        @Operation(summary = "Create a new business")
        public ResponseEntity<BusinessResponse> createBusiness(
                @Valid @RequestBody CreateBusinessRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(businessService.createBusiness(request));
        }

        @GetMapping("/my")
        @Operation(summary = "List all businesses owned by the current user")
        public ResponseEntity<List<BusinessResponse>> getMyBusinesses() {
                return ResponseEntity.ok(businessService.getMyBusinesses());
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a business by ID")
        public ResponseEntity<BusinessResponse> getBusinessById(@PathVariable Long id) {
                return ResponseEntity.ok(businessService.getBusinessById(id));
        }

        @GetMapping("/{id}/statistics")
        @Operation(summary = "Get business statistics")
        public ResponseEntity<BusinessStatisticsResponse> getStatistics(@PathVariable Long id) {
                return ResponseEntity.ok(statisticsService.getStatistics(id));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update business details")
        public ResponseEntity<BusinessResponse> updateBusiness(
                @PathVariable Long id,
                @Valid @RequestBody CreateBusinessRequest request) {
                return ResponseEntity.ok(businessService.updateBusiness(id, request));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Soft-delete a business")
        public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
                businessService.deleteBusiness(id);
                return ResponseEntity.noContent().build();
        }
}