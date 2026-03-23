package com.nitin.saas.staff.controller;
import com.nitin.saas.staff.dto.*;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.service.StaffInvitationService;
import com.nitin.saas.staff.service.StaffSalaryService;
import com.nitin.saas.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final StaffInvitationService invitationService;
    private final StaffSalaryService staffSalaryService;
    // ── Staff CRUD ────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Add staff member to business")
    public ResponseEntity<StaffResponse> addStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(staffService.addStaff(businessId, request));
    }
    @GetMapping
    @Operation(summary = "Get all staff members with pagination")
    public ResponseEntity<Page<StaffResponse>> getBusinessStaff(
            @PathVariable Long businessId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(staffService.getBusinessStaff(businessId, search, status, pageable));
    }
    @GetMapping("/list")
    @Operation(summary = "Get all staff members as list")
    public ResponseEntity<List<StaffResponse>> getBusinessStaffList(
            @PathVariable Long businessId) {
        return ResponseEntity.ok(staffService.getBusinessStaffList(businessId));
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get staff member by ID")
    public ResponseEntity<StaffResponse> getStaffById(@PathVariable String id) {
        return ResponseEntity.ok(staffService.getStaffById(id));
    }
    @GetMapping("/{id}/detail")
    @Operation(summary = "Get staff member detailed information")
    public ResponseEntity<StaffDetailResponse> getStaffDetail(@PathVariable String id) {
        return ResponseEntity.ok(staffService.getStaffDetail(id));
    }
    @GetMapping("/search")
    @Operation(summary = "Search staff by email, phone, or employee ID")
    public ResponseEntity<Page<StaffResponse>> searchStaff(
            @PathVariable Long businessId,
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(staffService.searchStaff(businessId, query, pageable));
    }
    @GetMapping("/status/{status}")
    @Operation(summary = "Get staff by status")
    public ResponseEntity<Page<StaffResponse>> getStaffByStatus(
            @PathVariable Long businessId,
            @PathVariable Staff.StaffStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(staffService.getStaffByStatus(businessId, status, pageable));
    }
    @PutMapping("/{id}")
    @Operation(summary = "Update staff member information")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable String id,
            @Valid @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }
    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate staff member")
    public ResponseEntity<Void> terminateStaff(
            @PathVariable String id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate) {
        staffService.terminateStaff(id, terminationDate);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend staff member")
    public ResponseEntity<Void> suspendStaff(@PathVariable String id) {
        staffService.suspendStaff(id);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate staff member")
    public ResponseEntity<Void> activateStaff(@PathVariable String id) {
        staffService.activateStaff(id);
        return ResponseEntity.ok().build();
    }
    // ── Permissions ───────────────────────────────────────────────────────────
    @PostMapping("/{staffId}/permissions/{permission}/grant")
    @Operation(summary = "Grant permission to staff member")
    public ResponseEntity<Void> grantPermission(
            @PathVariable String staffId,
            @PathVariable StaffRole.Permission permission) {
        staffService.grantPermission(staffId, permission);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{staffId}/permissions/{permission}/revoke")
    @Operation(summary = "Revoke permission from staff member")
    public ResponseEntity<Void> revokePermission(
            @PathVariable String staffId,
            @PathVariable StaffRole.Permission permission) {
        staffService.revokePermission(staffId, permission);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{staffId}/permissions")
    @Operation(summary = "Get effective permissions for staff member")
    public ResponseEntity<Set<StaffRole.Permission>> getEffectivePermissions(
            @PathVariable String staffId) {
        return ResponseEntity.ok(staffService.getEffectivePermissions(staffId));
    }
    @GetMapping("/{staffId}/permissions/{permission}/check")
    @Operation(summary = "Check if staff has specific permission")
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable String staffId,
            @PathVariable StaffRole.Permission permission) {
        return ResponseEntity.ok(staffService.hasPermission(staffId, permission));
    }
    @GetMapping("/count")
    @Operation(summary = "Count active staff members")
    public ResponseEntity<Long> countActiveStaff(@PathVariable Long businessId) {
        return ResponseEntity.ok(staffService.countActiveStaff(businessId));
    }

    @GetMapping("/salary-payments")
    @Operation(summary = "Get monthly staff salary payments")
    public ResponseEntity<List<StaffSalaryPaymentResponse>> getMonthlySalaryPayments(
            @PathVariable Long businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(staffSalaryService.getMonthlyPayments(businessId, month));
    }

    @GetMapping("/salary-payments/unpaid")
    @Operation(summary = "Get unpaid staff salary records")
    public ResponseEntity<List<StaffSalaryPaymentResponse>> getUnpaidSalaryPayments(
            @PathVariable Long businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(staffSalaryService.getUnpaidPayments(businessId, month));
    }

    @PostMapping("/{id}/salary/mark-paid")
    @Operation(summary = "Mark salary as paid for a month")
    public ResponseEntity<StaffSalaryPaymentResponse> markSalaryPaid(
            @PathVariable Long businessId,
            @PathVariable String id,
            @Valid @RequestBody MarkSalaryPaidRequest request) {
        return ResponseEntity.ok(staffSalaryService.markSalaryPaid(businessId, id, request));
    }
    // ── Invitations ───────────────────────────────────────────────────────────
    @PostMapping("/invite")
    @Operation(summary = "Invite a new staff member by email")
    public ResponseEntity<StaffInvitationResponse> inviteStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody InviteStaffRequest request,
            HttpServletRequest httpRequest) {
        StaffInvitationResponse response = invitationService.inviteStaff(businessId, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/invitations")
    @Operation(summary = "Get all invitations for business (paginated)")
    public ResponseEntity<Page<StaffInvitationResponse>> getBusinessInvitations(
            @PathVariable Long businessId,
            Pageable pageable) {
        return ResponseEntity.ok(invitationService.getBusinessInvitations(businessId, pageable));
    }
    @GetMapping("/invitations/pending")
    @Operation(summary = "Get pending (unused, unexpired) invitations")
    public ResponseEntity<List<StaffInvitationResponse>> getPendingInvitations(
            @PathVariable Long businessId) {
        return ResponseEntity.ok(invitationService.getPendingInvitations(businessId));
    }
    @PostMapping("/invitations/{invitationId}/resend")
    @Operation(summary = "Resend an invitation email")
    public ResponseEntity<Void> resendInvitation(
            @PathVariable Long businessId,
            @PathVariable Long invitationId,
            HttpServletRequest httpRequest) {
        invitationService.resendInvitation(invitationId, httpRequest);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/invitations/{invitationId}")
    @Operation(summary = "Cancel / delete an invitation")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable Long businessId,
            @PathVariable Long invitationId) {
        invitationService.cancelInvitation(invitationId);
        return ResponseEntity.noContent().build();
    }
}