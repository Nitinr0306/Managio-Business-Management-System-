package com.nitin.saas.auth.controller;

import com.nitin.saas.auth.dto.*;
import com.nitin.saas.auth.service.AuthService;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.common.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nitin.saas.staff.service.StaffAuthService;  // ADD THIS
import com.nitin.saas.auth.dto.StaffLoginRequest;     // ADD THIS
import com.nitin.saas.auth.dto.StaffLoginResponse;    // ADD THIS


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;
    private final RBACService rbacService;
    private final StaffAuthService staffAuthService;  // >>> ADD THIS FIELD <<<

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("Registration request received for email: {}", request.getEmail());
        UserResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login request received for email: {}", request.getEmail());
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthTokenResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }
        AuthTokenResponse response = authService.refreshToken(refreshToken, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Void> logout(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            HttpServletRequest httpRequest) {
        authService.logout(refreshToken, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<Void> forgotPassword(
            @RequestParam String email,
            HttpServletRequest httpRequest) {
        authService.requestPasswordReset(email, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            HttpServletRequest httpRequest) {
        authService.resetPassword(token, newPassword, httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<Void> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            HttpServletRequest httpRequest) {
        Long userId = rbacService.getCurrentUserId();
        authService.changePassword(userId, oldPassword, newPassword, httpRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    public ResponseEntity<UserResponse> getCurrentUser() {
        var user = rbacService.getCurrentUserEntity();
        return ResponseEntity.ok(UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .emailVerified(user.getEmailVerified())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .accountStatus(user.getAccountStatus().name())
                .profileImageUrl(user.getProfileImageUrl())
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build());
    }

    // ADD THIS METHOD TO AUTH CONTROLLER

    @PostMapping("/staff/login")
    @Operation(summary = "Staff login with business context")
    public ResponseEntity<StaffLoginResponse> staffLogin(
            @Valid @RequestBody StaffLoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Staff login request received for email: {}, businessId: {}",
                request.getEmail(), request.getBusinessId());
        StaffLoginResponse response = staffAuthService.staffLogin(request, httpRequest);
        return ResponseEntity.ok(response);
    }
}