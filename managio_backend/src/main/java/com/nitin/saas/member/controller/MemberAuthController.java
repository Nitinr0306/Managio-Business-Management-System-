package com.nitin.saas.member.controller;

import com.nitin.saas.member.dto.MemberLoginRequest;
import com.nitin.saas.member.dto.MemberLoginResponse;
import com.nitin.saas.member.dto.MemberRegisterResponse;
import com.nitin.saas.member.dto.MemberRegistrationRequest;
import com.nitin.saas.member.dto.ChangePasswordRequest;
import com.nitin.saas.common.dto.ResetPasswordRequest;
import com.nitin.saas.member.service.MemberAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/auth")
@RequiredArgsConstructor
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    @PostMapping("/register")
    public ResponseEntity<MemberRegisterResponse> register(
            @RequestBody MemberRegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                memberAuthService.memberRegister(request, httpRequest)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<MemberLoginResponse> login(
            @RequestBody MemberLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                memberAuthService.memberLogin(request, httpRequest)
        );
    }

    // 🔥 NEW
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        memberAuthService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    // 🔥 NEW
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestParam String email) {
        memberAuthService.resendVerification(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestParam String identifier,HttpServletRequest request
    ) {
        memberAuthService.requestPasswordReset(identifier,request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        memberAuthService.resetPassword(token, newPassword);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/reset-password", consumes = "application/json")
    public ResponseEntity<Void> resetPasswordBody(
            @Valid @RequestBody ResetPasswordRequest request) {
        memberAuthService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        memberAuthService.changePassword(request, httpRequest);
        return ResponseEntity.ok().build();
    }
}