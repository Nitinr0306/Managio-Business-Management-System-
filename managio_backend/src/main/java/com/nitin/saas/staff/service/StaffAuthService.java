package com.nitin.saas.staff.service;

import com.nitin.saas.auth.dto.StaffLoginRequest;
import com.nitin.saas.auth.dto.StaffLoginResponse;
import com.nitin.saas.auth.dto.UserResponse;
import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.exception.*;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.common.utils.IpAddressUtil;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAuthService {

    private final UserRepository         userRepository;
    private final StaffRepository        staffRepository;
    private final BusinessRepository     businessRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtil                jwtUtil;
    private final StaffService           staffService;

    @Value("${app.security.max-login-attempts:5}")
    private Integer maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private Integer accountLockDurationMinutes;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Transactional
    public StaffLoginResponse staffLogin(StaffLoginRequest request, HttpServletRequest httpRequest) {
        String email   = request.getEmail().toLowerCase();
        String ip      = IpAddressUtil.getClientIp(httpRequest);
        String ua      = httpRequest.getHeader("User-Agent");

        log.info("Staff login: email={}, businessId={}, ip={}", email, request.getBusinessId(), ip);

        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            logFailed(email, ip, ua, "User not found");
            return new BusinessException("Invalid credentials", ErrorCode.INVALID_CREDENTIALS);
        });

        // Check account lock
        if (user.getAccountLocked()) {
            if (shouldUnlock(user)) {
                user.unlockAccount();
                userRepository.save(user);
            } else {
                logFailed(email, ip, ua, "Account locked");
                throw new AccountLockedException("Account is locked. Please try again later.");
            }
        }

        if (!user.getEnabled()) {
            logFailed(email, ip, ua, "Account disabled");
            throw new BusinessException("Account is disabled", ErrorCode.ACCOUNT_DISABLED);
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            logFailed(email, ip, ua, "Email not verified");
            throw new BusinessException(
                    "Email not verified",
                    ErrorCode.EMAIL_NOT_VERIFIED
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ip, ua);
            throw new BusinessException(
                    "Invalid credentials",
                    ErrorCode.INVALID_CREDENTIALS
            );
        }

        Staff staff = staffRepository.findByBusinessIdAndUserId(request.getBusinessId(), user.getId())
                .orElseThrow(() -> {
                    logFailed(email, ip, ua, "Not staff in business " + request.getBusinessId());
                    return new BusinessException("Invalid credentials", ErrorCode.INVALID_CREDENTIALS);
                });

        if (!staff.isActive()) {
            logFailed(email, ip, ua, "Staff status=" + staff.getStatus());
            throw new BusinessException("Your staff account is not active", ErrorCode.ACCOUNT_DISABLED);
        }
        if (!staff.getCanLogin()) {
            logFailed(email, ip, ua, "Staff canLogin=false");
            throw new BusinessException("You do not have login permission", ErrorCode.AUTHORIZATION_ERROR);
        }
        if (staff.isDeleted()) {
            logFailed(email, ip, ua, "Staff record deleted");
            throw new BusinessException("Your staff account has been removed", ErrorCode.ACCOUNT_DISABLED);
        }

        // 2FA stub
        if (user.getTwoFactorEnabled() && request.getTwoFactorCode() == null) {
            return StaffLoginResponse.builder().requiresTwoFactor(true)
                    .message("Two-factor authentication required").build();
        }

        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String       accessToken  = jwtUtil.generateStaffAccessToken(user, staff);
        RefreshToken refreshToken = createRefreshToken(user, request.getDeviceId(), ip, ua);

        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Set<StaffRole.Permission> effectivePermissions =
                staffService.getEffectivePermissions(staff.getId());

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(email)
                .eventType(AuthAuditLog.EventType.LOGIN_SUCCESS)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .details("Staff login to business " + business.getId() + " as " + staff.getRole())
                .build());

        log.info("Staff login successful: userId={}, businessId={}, staffId={}",
                user.getId(), business.getId(), staff.getId());

        return StaffLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .user(mapToUserResponse(user))
                .staff(mapToStaffInfo(staff, effectivePermissions))
                .business(mapToBusinessInfo(business))
                .requiresTwoFactor(false)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void handleFailedLogin(User user, String ip, String ua) {
        user.incrementFailedAttempts();
        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lockAccount();
            authAuditLogRepository.save(AuthAuditLog.builder()
                    .userId(user.getId()).email(user.getEmail())
                    .eventType(AuthAuditLog.EventType.ACCOUNT_LOCKED)
                    .status(AuthAuditLog.Status.BLOCKED)
                    .ipAddress(ip).userAgent(ua).details("Max attempts exceeded").build());
        }
        userRepository.save(user);
        logFailed(user.getEmail(), ip, ua, "Invalid password");
    }

    private void logFailed(String email, String ip, String ua, String reason) {
        authAuditLogRepository.save(AuthAuditLog.loginFailed(email, ip, ua, reason));
    }

    private boolean shouldUnlock(User user) {
        if (user.getLockedAt() == null) return false;
        return user.getLockedAt().plusMinutes(accountLockDurationMinutes)
                .isBefore(LocalDateTime.now());
    }

    private RefreshToken createRefreshToken(User user, String deviceId, String ip, String ua) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString()).userId(user.getId()).subjectType("USER")
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .deviceId(deviceId).ipAddress(ip).userAgent(ua).build());
    }

    private UserResponse mapToUserResponse(User u) {
        return UserResponse.builder().id(u.getId()).email(u.getEmail())
                .firstName(u.getFirstName()).lastName(u.getLastName()).fullName(u.getFullName())
                .phoneNumber(u.getPhoneNumber()).roles(u.getRoles())
                .emailVerified(u.getEmailVerified()).enabled(u.getEnabled())
                .accountLocked(u.getAccountLocked()).accountStatus(u.getAccountStatus().name())
                .twoFactorEnabled(u.getTwoFactorEnabled()).lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt()).build();
    }

    private StaffLoginResponse.StaffInfo mapToStaffInfo(Staff s, Set<StaffRole.Permission> perms) {
        return StaffLoginResponse.StaffInfo.builder()
                .staffId(s.getId()).businessId(s.getBusinessId()).role(s.getRole())
                .roleDisplay(s.getRole().getDisplayName()).status(s.getStatus())
                .department(s.getDepartment()).designation(s.getDesignation())
                .employeeId(s.getEmployeeId()).permissions(perms)
                .canLogin(s.getCanLogin()).canManageMembers(s.getCanManageMembers())
                .canManagePayments(s.getCanManagePayments())
                .canManageSubscriptions(s.getCanManageSubscriptions())
                .canViewReports(s.getCanViewReports()).build();
    }

    private StaffLoginResponse.BusinessInfo mapToBusinessInfo(Business b) {
        return StaffLoginResponse.BusinessInfo.builder()
                .id(b.getId()).name(b.getName()).address(b.getAddress())
                .phone(b.getPhone()).email(b.getEmail()).build();
    }
}