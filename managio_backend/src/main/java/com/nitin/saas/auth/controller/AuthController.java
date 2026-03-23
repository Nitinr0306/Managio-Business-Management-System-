package com.nitin.saas.auth.controller;

import com.nitin.saas.auth.dto.*;
import com.nitin.saas.auth.service.AuthService;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.common.dto.ResetPasswordRequest;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.staff.service.StaffAuthService;
import com.nitin.saas.member.service.MemberAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration, login, token management")
public class AuthController {

    private final AuthService      authService;
    private final RBACService      rbacService;
    private final StaffAuthService staffAuthService;
    private final MemberAuthService memberAuthService;

    @PostMapping("/register")
    @Operation(summary = "Register a new owner account",
            description = "Creates a new user account. Returns the user profile. A verification email is sent automatically.")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("Register attempt: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, httpRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password",
            description = "Returns access + refresh tokens. If email is unverified a new verification email is sent and 401 is returned.")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login attempt: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
            description = "Pass the refresh token in X-Refresh-Token header. Returns a new access token and rotates the refresh token.")
    public ResponseEntity<AuthTokenResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token header is required");
        }
        return ResponseEntity.ok(authService.refreshToken(refreshToken, httpRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoke tokens",
            description = "Revokes the refresh token and blacklists the access token. Both headers are needed for a clean logout.")
    public ResponseEntity<Void> logout(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        authService.logout(refreshToken, accessToken, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address",
            description = "Pass the token from the verification email. Activates the account.")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification-email")
    @Operation(summary = "resend verification email",
               description = "Resends a verification email to the user to confirm the email address.")
    public ResponseEntity<Void> resendVerificationEmail(@RequestParam String email, HttpServletRequest httpRequest) {
        authService.resendVerificationEmail(email, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email",
            description = "Always returns 200 regardless of whether the email exists (prevents enumeration).")
    public ResponseEntity<Void> forgotPassword(
            @RequestParam String email,
            HttpServletRequest httpRequest) {
        authService.requestPasswordReset(email, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using email token")
    public ResponseEntity<Void> resetPassword(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String subject,
            HttpServletRequest httpRequest) {

        if (token == null || token.isBlank()) {
            throw new BadRequestException("Required parameter 'token' is missing");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BadRequestException("Required parameter 'newPassword' is missing");
        }

        resetPasswordBySubject(token, newPassword, subject, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/reset-password", consumes = "application/json")
    @Operation(summary = "Reset password using token payload")
    public ResponseEntity<Void> resetPasswordBody(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        resetPasswordBySubject(request.getToken(), request.getNewPassword(), request.getSubject(), httpRequest);
        return ResponseEntity.ok().build();
    }

    private void resetPasswordBySubject(String token,
                                        String newPassword,
                                        String subject,
                                        HttpServletRequest httpRequest) {

        String normalizedSubject = subject == null ? "" : subject.trim().toLowerCase();

        if ("member".equals(normalizedSubject)) {
            memberAuthService.resetPassword(token, newPassword);
            return;
        }

        if ("user".equals(normalizedSubject) || normalizedSubject.isBlank()) {
            try {
                authService.resetPassword(token, newPassword, httpRequest);
                return;
            } catch (BadRequestException ex) {
                // Backward-compatible fallback for clients that hit /auth/reset-password
                // with a member token.
                if (normalizedSubject.isBlank() && "Invalid reset token".equals(ex.getMessage())) {
                    memberAuthService.resetPassword(token, newPassword);
                    return;
                }
                throw ex;
            }
        }

        throw new BadRequestException("Invalid subject. Supported values: user, member");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (authenticated)",
            description = "Requires current password. Revokes all existing sessions on success.")
    public ResponseEntity<Void> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            HttpServletRequest httpRequest) {
        Long userId = rbacService.getCurrentUserId();
        authService.changePassword(userId, oldPassword, newPassword, httpRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile")
    public ResponseEntity<UserResponse> getCurrentUser() {
        var user = rbacService.getCurrentUserEntity();
        return ResponseEntity.ok(authService.mapToUserResponse(user));
    }

    @PostMapping("/staff/login")
    @Operation(summary = "Staff login with business context",
            description = "Returns a staff-scoped JWT that includes businessId, staffRole, and permission flags.")
    public ResponseEntity<StaffLoginResponse> staffLogin(
            @Valid @RequestBody StaffLoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Staff login: email={}, businessId={}", request.getEmail(), request.getBusinessId());
        return ResponseEntity.ok(staffAuthService.staffLogin(request, httpRequest));
    }
}