package com.nitin.saas.staff.controller;

import com.nitin.saas.staff.dto.*;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "Staff management endpoints")
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @Operation(summary = "Add staff member to business")
    public ResponseEntity<StaffResponse> addStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateStaffRequest request) {
        StaffResponse response = staffService.addStaff(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all staff members with pagination")
    public ResponseEntity<Page<StaffResponse>> getBusinessStaff(
            @PathVariable Long businessId,
            Pageable pageable) {
        Page<StaffResponse> response = staffService.getBusinessStaff(businessId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @Operation(summary = "Get all staff members as list")
    public ResponseEntity<List<StaffResponse>> getBusinessStaffList(
            @PathVariable Long businessId) {
        List<StaffResponse> response = staffService.getBusinessStaffList(businessId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get staff member by ID")
    public ResponseEntity<StaffResponse> getStaffById(@PathVariable Long id) {
        StaffResponse response = staffService.getStaffById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "Get staff member detailed information")
    public ResponseEntity<StaffDetailResponse> getStaffDetail(@PathVariable Long id) {
        StaffDetailResponse response = staffService.getStaffDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search staff by email, phone, or employee ID")
    public ResponseEntity<Page<StaffResponse>> searchStaff(
            @PathVariable Long businessId,
            @RequestParam String query,
            Pageable pageable) {
        Page<StaffResponse> response = staffService.searchStaff(businessId, query, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get staff by status")
    public ResponseEntity<Page<StaffResponse>> getStaffByStatus(
            @PathVariable Long businessId,
            @PathVariable Staff.StaffStatus status,
            Pageable pageable) {
        Page<StaffResponse> response = staffService.getStaffByStatus(businessId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update staff member information")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request) {
        StaffResponse response = staffService.updateStaff(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate staff member")
    public ResponseEntity<Void> terminateStaff(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate) {
        staffService.terminateStaff(id, terminationDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend staff member")
    public ResponseEntity<Void> suspendStaff(@PathVariable Long id) {
        staffService.suspendStaff(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate staff member")
    public ResponseEntity<Void> activateStaff(@PathVariable Long id) {
        staffService.activateStaff(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{staffId}/permissions/{permission}/grant")
    @Operation(summary = "Grant permission to staff member")
    public ResponseEntity<Void> grantPermission(
            @PathVariable Long staffId,
            @PathVariable StaffRole.Permission permission) {
        staffService.grantPermission(staffId, permission);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{staffId}/permissions/{permission}/revoke")
    @Operation(summary = "Revoke permission from staff member")
    public ResponseEntity<Void> revokePermission(
            @PathVariable Long staffId,
            @PathVariable StaffRole.Permission permission) {
        staffService.revokePermission(staffId, permission);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{staffId}/permissions")
    @Operation(summary = "Get effective permissions for staff member")
    public ResponseEntity<Set<StaffRole.Permission>> getEffectivePermissions(
            @PathVariable Long staffId) {
        Set<StaffRole.Permission> permissions = staffService.getEffectivePermissions(staffId);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{staffId}/permissions/{permission}/check")
    @Operation(summary = "Check if staff has specific permission")
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable Long staffId,
            @PathVariable StaffRole.Permission permission) {
        boolean hasPermission = staffService.hasPermission(staffId, permission);
        return ResponseEntity.ok(hasPermission);
    }

    @GetMapping("/count")
    @Operation(summary = "Count active staff members")
    public ResponseEntity<Long> countActiveStaff(@PathVariable Long businessId) {
        Long count = staffService.countActiveStaff(businessId);
        return ResponseEntity.ok(count);
    }
}