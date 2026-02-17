package com.nitin.saas.staff.controller;

import com.nitin.saas.staff.dto.AcceptInvitationRequest;
import com.nitin.saas.staff.dto.InviteStaffRequest;
import com.nitin.saas.staff.dto.StaffInvitationResponse;
import com.nitin.saas.staff.service.StaffInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Invitations", description = "Staff invitation management")
public class StaffInvitationController {

    private final StaffInvitationService invitationService;

    @PostMapping("/businesses/{businessId}/invite")
    @Operation(summary = "Invite staff member to business")
    public ResponseEntity<StaffInvitationResponse> inviteStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody InviteStaffRequest request,
            HttpServletRequest httpRequest) {
        StaffInvitationResponse response = invitationService.inviteStaff(businessId, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accept-invitation")
    @Operation(summary = "Accept staff invitation")
    public ResponseEntity<StaffInvitationResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            HttpServletRequest httpRequest) {
        StaffInvitationResponse response = invitationService.acceptInvitation(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitation")
    @Operation(summary = "Get invitation details by token")
    public ResponseEntity<StaffInvitationResponse> getInvitationByToken(
            @RequestParam String token) {
        StaffInvitationResponse response = invitationService.getInvitationByToken(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/businesses/{businessId}/invitations")
    @Operation(summary = "Get all invitations for business")
    public ResponseEntity<Page<StaffInvitationResponse>> getBusinessInvitations(
            @PathVariable Long businessId,
            Pageable pageable) {
        Page<StaffInvitationResponse> response = invitationService.getBusinessInvitations(businessId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/businesses/{businessId}/invitations/pending")
    @Operation(summary = "Get pending invitations for business")
    public ResponseEntity<List<StaffInvitationResponse>> getPendingInvitations(
            @PathVariable Long businessId) {
        List<StaffInvitationResponse> response = invitationService.getPendingInvitations(businessId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/{invitationId}/resend")
    @Operation(summary = "Resend staff invitation")
    public ResponseEntity<Void> resendInvitation(
            @PathVariable Long invitationId,
            HttpServletRequest httpRequest) {
        invitationService.resendInvitation(invitationId, httpRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/invitations/{invitationId}")
    @Operation(summary = "Cancel staff invitation")
    public ResponseEntity<Void> cancelInvitation(@PathVariable Long invitationId) {
        invitationService.cancelInvitation(invitationId);
        return ResponseEntity.noContent().build();
    }
}