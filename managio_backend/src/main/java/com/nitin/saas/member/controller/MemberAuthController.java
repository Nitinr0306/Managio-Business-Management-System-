package com.nitin.saas.member.controller;

import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.dto.ChangePasswordRequest;
import com.nitin.saas.member.dto.MemberLoginRequest;
import com.nitin.saas.member.dto.MemberLoginResponse;
import com.nitin.saas.member.dto.MemberRegistrationRequest;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.member.service.MemberAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/auth")
@RequiredArgsConstructor
@Tag(name = "Member Authentication", description = "Member self-service authentication endpoints")
public class MemberAuthController {

    private final MemberAuthService memberAuthService;
    private final RBACService       rbacService;
    private final MemberRepository  memberRepository;
    private final BusinessService   businessService;

    // ── Public endpoints (no JWT required) ────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Member self-registration",
            description = "Register a new member account linked to a specific business. "
                    + "Returns tokens immediately — no email verification required.")
    public ResponseEntity<MemberLoginResponse> register(
            @Valid @RequestBody MemberRegistrationRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberAuthService.memberRegister(request, httpRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "Member login",
            description = "Login using phone number or email address plus password.")
    public ResponseEntity<MemberLoginResponse> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(memberAuthService.memberLogin(request, httpRequest));
    }

    /**
     * Always returns 200 regardless of whether the identifier exists.
     * This prevents enumeration of registered member phone numbers / emails.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset",
            description = "Send a password reset link to the member's email. "
                    + "Identifier can be phone number or email. Always returns 200.")
    public ResponseEntity<Void> forgotPassword(
            @RequestParam String identifier,
            HttpServletRequest httpRequest) {
        memberAuthService.requestPasswordReset(identifier, httpRequest);
        return ResponseEntity.ok().build();
    }

    /**
     * Completes the password reset using the token from the email link.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password via email link",
            description = "Use the token from the reset email to set a new password.")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            HttpServletRequest httpRequest) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }
        memberAuthService.resetPassword(token, newPassword, httpRequest);
        return ResponseEntity.ok().build();
    }

    // ── Authenticated endpoints (JWT required) ────────────────────────────────

    /**
     * FIX B2: memberId is derived from the JWT principal — not from a request param.
     * This prevents a member from changing another member's password.
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change member password",
            description = "Change the password for the currently authenticated member.")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long memberId = rbacService.getCurrentUserId();
        memberAuthService.changePassword(memberId, request);
        return ResponseEntity.ok().build();
    }

    // ── Admin / Staff endpoints ───────────────────────────────────────────────

    /**
     * FIX B3: verifies business access before allowing account disable.
     */
    @PostMapping("/{memberId}/disable")
    @Operation(summary = "Disable a member account",
            description = "Requires business owner or active staff access.")
    public ResponseEntity<Void> disableAccount(@PathVariable Long memberId) {
        requireBusinessAccessForMember(memberId);
        memberAuthService.disableMemberAccount(memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{memberId}/enable")
    @Operation(summary = "Enable a member account",
            description = "Requires business owner or active staff access.")
    public ResponseEntity<Void> enableAccount(@PathVariable Long memberId) {
        requireBusinessAccessForMember(memberId);
        memberAuthService.enableMemberAccount(memberId);
        return ResponseEntity.ok().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void requireBusinessAccessForMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
        businessService.requireAccess(member.getBusinessId());
    }
}