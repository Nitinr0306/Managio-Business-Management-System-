package com.nitin.saas.member.controller;

import com.nitin.saas.member.dto.ChangePasswordRequest;
import com.nitin.saas.member.dto.MemberLoginRequest;
import com.nitin.saas.member.dto.MemberLoginResponse;
import com.nitin.saas.member.dto.MemberRegistrationRequest;
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
@Tag(name = "Member Authentication", description = "Member authentication and self-service endpoints")
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    @PostMapping("/register")
    @Operation(summary = "Member self-registration")
    public ResponseEntity<MemberLoginResponse> register(
            @Valid @RequestBody MemberRegistrationRequest request,
            HttpServletRequest httpRequest) {
        MemberLoginResponse response = memberAuthService.memberRegister(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Member login")
    public ResponseEntity<MemberLoginResponse> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletRequest httpRequest) {
        MemberLoginResponse response = memberAuthService.memberLogin(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change member password")
    public ResponseEntity<Void> changePassword(
            @RequestParam Long memberId,
            @Valid @RequestBody ChangePasswordRequest request) {
        memberAuthService.changePassword(memberId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<Void> forgotPassword(
            @RequestParam String identifier,
            HttpServletRequest httpRequest) {
        memberAuthService.requestPasswordReset(identifier, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{memberId}/disable")
    @Operation(summary = "Disable member account")
    public ResponseEntity<Void> disableAccount(@PathVariable Long memberId) {
        memberAuthService.disableMemberAccount(memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{memberId}/enable")
    @Operation(summary = "Enable member account")
    public ResponseEntity<Void> enableAccount(@PathVariable Long memberId) {
        memberAuthService.enableMemberAccount(memberId);
        return ResponseEntity.ok().build();
    }
}