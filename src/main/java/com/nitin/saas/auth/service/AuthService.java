package com.nitin.saas.auth.service;

import com.nitin.saas.auth.dto.*;
import com.nitin.saas.auth.entity.*;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.*;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ConflictException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.JwtUtil;
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
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.security.max-login-attempts:5}")
    private Integer maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private Integer accountLockDurationMinutes;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Value("${app.security.email-verification-expiry-hours:24}")
    private Integer emailVerificationExpiryHours;

    @Value("${app.security.password-reset-expiry-hours:1}")
    private Integer passwordResetExpiryHours;

    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new ConflictException("Email already registered");
        }

        validatePasswordStrength(request.getPassword());

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .preferredLanguage(request.getPreferredLanguage())
                .timezone(request.getTimezone())
                .roles(Set.of(Role.USER))
                .emailVerified(false)
                .enabled(true)
                .accountLocked(false)
                .accountStatus(User.AccountStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        createEmailVerificationToken(user, httpRequest);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.REGISTER)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(getUserAgent(httpRequest))
                .build());

        return mapToUserResponse(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase();
        String ipAddress = getClientIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);

        log.info("Login attempt for email: {} from IP: {}", email, ipAddress);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    authAuditLogRepository.save(AuthAuditLog.loginFailed(
                            email, ipAddress, userAgent, "User not found"));
                    return new BadCredentialsException("Invalid credentials");
                });

        if (user.getAccountLocked()) {
            if (shouldUnlockAccount(user)) {
                user.unlockAccount();
                userRepository.save(user);
            } else {
                authAuditLogRepository.save(AuthAuditLog.builder()
                        .userId(user.getId())
                        .email(email)
                        .eventType(AuthAuditLog.EventType.LOGIN_FAILED)
                        .status(AuthAuditLog.Status.BLOCKED)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .errorMessage("Account is locked")
                        .build());
                throw new LockedException("Account is locked. Please try again later.");
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            authAuditLogRepository.save(AuthAuditLog.loginFailed(
                    email, ipAddress, userAgent, "Account disabled"));
            throw new BadCredentialsException("Account is disabled");
        }

        if (user.getTwoFactorEnabled() && request.getTwoFactorCode() == null) {
            return LoginResponse.builder()
                    .requiresTwoFactor(true)
                    .message("Two-factor authentication required")
                    .build();
        }

        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user, request.getDeviceId(),
                ipAddress, userAgent);

        authAuditLogRepository.save(AuthAuditLog.loginSuccess(
                user.getId(), email, ipAddress, userAgent));

        log.info("Login successful for user: {}", email);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .user(mapToUserResponse(user))
                .requiresTwoFactor(false)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Transactional
    public AuthTokenResponse refreshToken(String refreshTokenValue, HttpServletRequest httpRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            log.warn("Invalid refresh token used: {}", refreshToken.getId());
            throw new BadRequestException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(user);

        String newRefreshTokenValue = UUID.randomUUID().toString();
        RefreshToken newRefreshToken = createRefreshToken(user, refreshToken.getDeviceId(),
                getClientIp(httpRequest), getUserAgent(httpRequest));

        refreshToken.rotate(newRefreshToken.getToken());
        refreshTokenRepository.save(refreshToken);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.TOKEN_REFRESHED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(getUserAgent(httpRequest))
                .build());

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue, HttpServletRequest httpRequest) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);

            authAuditLogRepository.save(AuthAuditLog.builder()
                    .userId(token.getUserId())
                    .eventType(AuthAuditLog.EventType.LOGOUT)
                    .status(AuthAuditLog.Status.SUCCESS)
                    .ipAddress(getClientIp(httpRequest))
                    .userAgent(getUserAgent(httpRequest))
                    .build());
        });
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (!verificationToken.isValid()) {
            throw new BadRequestException("Verification token is expired or already used");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);

        verificationToken.markAsUsed();
        emailVerificationTokenRepository.save(verificationToken);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.EMAIL_VERIFIED)
                .status(AuthAuditLog.Status.SUCCESS)
                .build());

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(passwordResetExpiryHours))
                .requestIpAddress(getClientIp(httpRequest))
                .requestUserAgent(getUserAgent(httpRequest))
                .build();

        passwordResetTokenRepository.save(resetToken);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_REQUESTED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(getUserAgent(httpRequest))
                .build());

        log.info("Password reset requested for user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String token, String newPassword, HttpServletRequest httpRequest) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (!resetToken.isValid()) {
            throw new BadRequestException("Reset token is expired or already used");
        }

        validatePasswordStrength(newPassword);

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.resetFailedAttempts();
        userRepository.save(user);

        resetToken.markAsUsed(getClientIp(httpRequest), getUserAgent(httpRequest));
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_SUCCESS)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(getUserAgent(httpRequest))
                .build());

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword,
                               HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        validatePasswordStrength(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_CHANGED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(getUserAgent(httpRequest))
                .build());

        log.info("Password changed for user: {}", user.getEmail());
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

    private void createEmailVerificationToken(User user, HttpServletRequest httpRequest) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(emailVerificationExpiryHours))
                .requestIpAddress(getClientIp(httpRequest))
                .requestUserAgent(getUserAgent(httpRequest))
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.EMAIL_VERIFICATION_SENT)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(getUserAgent(httpRequest))
                .build());

        log.info("Email verification token created for user: {}", user.getEmail());
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

        authAuditLogRepository.save(AuthAuditLog.loginFailed(
                user.getEmail(), ipAddress, userAgent, "Invalid password"));
    }

    private boolean shouldUnlockAccount(User user) {
        if (user.getLockedAt() == null) {
            return false;
        }
        return user.getLockedAt()
                .plusMinutes(accountLockDurationMinutes)
                .isBefore(LocalDateTime.now());
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch ->
                "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BadRequestException(
                    "Password must contain uppercase, lowercase, digit, and special character");
        }
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