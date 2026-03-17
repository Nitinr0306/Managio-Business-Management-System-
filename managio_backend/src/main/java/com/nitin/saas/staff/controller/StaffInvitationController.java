package com.nitin.saas.staff.controller;

import com.nitin.saas.staff.dto.AcceptInvitationRequest;
import com.nitin.saas.staff.dto.StaffInvitationResponse;
import com.nitin.saas.staff.service.StaffInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FIX 12 + 10: The original controller had base path /api/v1/staff/businesses/{id}/invite
 * which clashed with the frontend's /api/v1/businesses/{id}/staff/invite.
 *
 * Invite and list-invitations endpoints have been moved into StaffController (under
 * /api/v1/businesses/{businessId}/staff) so those paths now match exactly.
 *
 * This controller keeps ONLY the two endpoints that are:
 *   (a) not scoped to a specific business in the URL, AND
 *   (b) called by unauthenticated users (accept, token lookup).
 *
 * FIX 10: /api/v1/staff/accept-invitation was missing from SecurityConfig.permitAll()
 * so unauthenticated users got 403. SecurityConfig.java is fixed separately.
 */
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Invitations (public)", description = "Public invitation endpoints")
public class StaffInvitationController {

    private final StaffInvitationService invitationService;

    /**
     * Accept an invitation.  Called by the unauthenticated accept-invitation page.
     * Must be in SecurityConfig.permitAll().
     */
    @PostMapping("/accept-invitation")
    @Operation(summary = "Accept a staff invitation and create an account")
    public ResponseEntity<StaffInvitationResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            HttpServletRequest httpRequest) {
        StaffInvitationResponse response = invitationService.acceptInvitation(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Look up invitation details by token.  Used by the accept-invitation page to
     * pre-fill the business name / role before the user submits the form.
     * Also unauthenticated.
     */
    @GetMapping("/invitation")
    @Operation(summary = "Get invitation details by token")
    public ResponseEntity<StaffInvitationResponse> getInvitationByToken(
            @RequestParam String token) {
        StaffInvitationResponse response = invitationService.getInvitationByToken(token);
        return ResponseEntity.ok(response);
    }
}