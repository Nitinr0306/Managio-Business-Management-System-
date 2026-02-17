package com.nitin.saas.staff.service;

import com.nitin.saas.auth.dto.AuthTokenResponse;
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
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
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

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final BusinessRepository businessRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StaffService staffService;

    @Value("${app.security.max-login-attempts:5}")
    private Integer maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private Integer accountLockDurationMinutes;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Transactional
    public StaffLoginResponse staffLogin(StaffLoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase();
        String ipAddress = getClientIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);

        log.info("Staff login attempt: email={}, businessId={}, ip={}",
                email, request.getBusinessId(), ipAddress);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logFailedLogin(email, ipAddress, userAgent, "User not found");
                    return new BadCredentialsException("Invalid credentials");
                });

        // Check account status
        validateUserAccount(user, email, ipAddress, userAgent);

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Find staff record for this business
        Staff staff = staffRepository.findByBusinessIdAndUserId(request.getBusinessId(), user.getId())
                .orElseThrow(() -> {
                    logFailedLogin(email, ipAddress, userAgent,
                            "User is not staff in business " + request.getBusinessId());
                    return new BadCredentialsException("You are not staff in this business");
                });

        // Validate staff status
        validateStaffStatus(staff, email, ipAddress, userAgent);

        // Check 2FA if enabled
        if (user.getTwoFactorEnabled() && request.getTwoFactorCode() == null) {
            return StaffLoginResponse.builder()
                    .requiresTwoFactor(true)
                    .message("Two-factor authentication required")
                    .build();
        }

        // Update user login info
        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens with business context
        String accessToken = jwtUtil.generateStaffAccessToken(user, staff);
        RefreshToken refreshToken = createRefreshToken(user, request.getDeviceId(),
                ipAddress, userAgent);

        // Get business info
        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Get effective permissions
        Set<StaffRole.Permission> effectivePermissions = staffService.getEffectivePermissions(staff.getId());

        // Log successful login
        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(email)
                .eventType(AuthAuditLog.EventType.LOGIN_SUCCESS)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(String.format("Staff login to business %d as %s", business.getId(), staff.getRole()))
                .build());

        log.info("Staff login successful: userId={}, businessId={}, staffId={}, role={}",
                user.getId(), business.getId(), staff.getId(), staff.getRole());

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

    @Transactional
    public AuthTokenResponse refreshStaffToken(String refreshTokenValue, HttpServletRequest httpRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            log.warn("Invalid refresh token used: {}", refreshToken.getId());
            throw new BadRequestException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Extract business ID and staff info from metadata or create new token with same context
        // For now, generate regular access token - in production, you'd want to preserve business context
        String newAccessToken = jwtUtil.generateAccessToken(user);

        String newRefreshTokenValue = UUID.randomUUID().toString();
        RefreshToken newRefreshToken = createRefreshToken(user, refreshToken.getDeviceId(),
                getClientIp(httpRequest), getUserAgent(httpRequest));

        refreshToken.rotate(newRefreshToken.getToken());
        refreshTokenRepository.save(refreshToken);

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .build();
    }

    private void validateUserAccount(User user, String email, String ipAddress, String userAgent) {
        if (user.getAccountLocked()) {
            if (shouldUnlockAccount(user)) {
                user.unlockAccount();
                userRepository.save(user);
            } else {
                logFailedLogin(email, ipAddress, userAgent, "Account is locked");
                throw new LockedException("Account is locked. Please try again later.");
            }
        }

        if (!user.getEnabled()) {
            logFailedLogin(email, ipAddress, userAgent, "Account disabled");
            throw new BadCredentialsException("Account is disabled");
        }
    }

    private void validateStaffStatus(Staff staff, String email, String ipAddress, String userAgent) {
        if (!staff.isActive()) {
            logFailedLogin(email, ipAddress, userAgent,
                    "Staff status is " + staff.getStatus());
            throw new BadCredentialsException("Your staff account is not active");
        }

        if (!staff.getCanLogin()) {
            logFailedLogin(email, ipAddress, userAgent, "Staff cannot login");
            throw new BadCredentialsException("You do not have login permission");
        }

        if (staff.isDeleted()) {
            logFailedLogin(email, ipAddress, userAgent, "Staff record deleted");
            throw new BadCredentialsException("Your staff account has been removed");
        }
    }

    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lockAccount();
            log.warn("Account locked due to failed login attempts: {}", user.getEmail());

            authAuditLogRepository.save(AuthAuditLog.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .eventType(AuthAuditLog.EventType.ACCOUNT_LOCKED)
                    .status(AuthAuditLog.Status.BLOCKED)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details("Max login attempts exceeded")
                    .build());
        }

        userRepository.save(user);
        logFailedLogin(user.getEmail(), ipAddress, userAgent, "Invalid password");
    }

    private void logFailedLogin(String email, String ipAddress, String userAgent, String reason) {
        authAuditLogRepository.save(AuthAuditLog.loginFailed(email, ipAddress, userAgent, reason));
    }

    private boolean shouldUnlockAccount(User user) {
        if (user.getLockedAt() == null) {
            return false;
        }
        return user.getLockedAt()
                .plusMinutes(accountLockDurationMinutes)
                .isBefore(LocalDateTime.now());
    }

    private RefreshToken createRefreshToken(User user, String deviceId, String ipAddress,
                                            String userAgent) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
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
                .build();
    }

    private StaffLoginResponse.StaffInfo mapToStaffInfo(Staff staff, Set<StaffRole.Permission> permissions) {
        return StaffLoginResponse.StaffInfo.builder()
                .staffId(staff.getId())
                .businessId(staff.getBusinessId())
                .role(staff.getRole())
                .roleDisplay(staff.getRole().getDisplayName())
                .status(staff.getStatus())
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .employeeId(staff.getEmployeeId())
                .permissions(permissions)
                .canLogin(staff.getCanLogin())
                .canManageMembers(staff.getCanManageMembers())
                .canManagePayments(staff.getCanManagePayments())
                .canManageSubscriptions(staff.getCanManageSubscriptions())
                .canViewReports(staff.getCanViewReports())
                .build();
    }

    private StaffLoginResponse.BusinessInfo mapToBusinessInfo(Business business) {
        return StaffLoginResponse.BusinessInfo.builder()
                .id(business.getId())
                .name(business.getName())
                .address(business.getAddress())
                .phone(business.getPhone())
                .email(business.getEmail())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}