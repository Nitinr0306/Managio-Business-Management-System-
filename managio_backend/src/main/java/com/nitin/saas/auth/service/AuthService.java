package com.nitin.saas.auth.service;

import com.nitin.saas.auth.dto.*;
import com.nitin.saas.auth.entity.*;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.*;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.AccountLockedException;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ConflictException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.common.security.TokenBlacklistService;
import com.nitin.saas.common.utils.IpAddressUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository                   userRepository;
    private final RefreshTokenRepository           refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository     passwordResetTokenRepository;
    private final AuthAuditLogRepository           authAuditLogRepository;
    private final PasswordEncoder                  passwordEncoder;
    private final JwtUtil                          jwtUtil;
    private final EmailNotificationService         emailService;
    private final TokenBlacklistService            tokenBlacklistService;
    private final Environment                      environment;

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

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new ConflictException("An account with this email already exists");
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
                // Hibernate may need to mutate this collection during merge; keep it mutable.
                .roles(new HashSet<>(Set.of(Role.USER)))
                .emailVerified(false)
                .enabled(true)
                .accountLocked(false)
                .accountStatus(User.AccountStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        createAndSendEmailVerificationToken(user, httpRequest);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .eventType(AuthAuditLog.EventType.REGISTER)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                .userAgent(ua(httpRequest))
                .build());

        return mapToUserResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional(noRollbackFor = {BadCredentialsException.class, AccountLockedException.class})
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email    = request.getEmail().toLowerCase();
        String ip       = IpAddressUtil.getClientIp(httpRequest);
        String userAgent = ua(httpRequest);

        log.info("Login attempt: email={}, ip={}", email, ip);

        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            authAuditLogRepository.save(AuthAuditLog.loginFailed(email, ip, userAgent, "User not found"));
            return new BadCredentialsException("Invalid credentials");
        });

        // Locked account — try auto-unlock first
        if (user.getAccountLocked()) {
            if (shouldUnlock(user)) {
                user.unlockAccount();
                userRepository.save(user);
            } else {
                authAuditLogRepository.save(AuthAuditLog.builder()
                        .userId(user.getId()).email(email)
                        .eventType(AuthAuditLog.EventType.LOGIN_FAILED)
                        .status(AuthAuditLog.Status.BLOCKED)
                        .ipAddress(ip).userAgent(userAgent)
                        .errorMessage("Account locked")
                        .build());
                throw new AccountLockedException("Account is locked. Please try again later.");
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ip, userAgent);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            authAuditLogRepository.save(AuthAuditLog.loginFailed(email, ip, userAgent, "Account disabled"));
            throw new BadCredentialsException("Account is disabled");
        }

        // Resend verification email if needed
        if (!user.getEmailVerified() && !isTestProfile()) {
            log.info("Resending verification email to unverified user: {}", email);
            createAndSendEmailVerificationToken(user, httpRequest);
            authAuditLogRepository.save(
                    AuthAuditLog.loginFailed(email, ip, userAgent, "Email not verified"));
            throw new BadCredentialsException(
                    "Email not verified. A new verification email has been sent to " + email);
        }

        // 2FA stub (token not yet fully implemented)
        if (user.getTwoFactorEnabled() && request.getTwoFactorCode() == null) {
            return LoginResponse.builder()
                    .requiresTwoFactor(true)
                    .message("Two-factor authentication code required")
                    .build();
        }

        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String       accessToken  = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user, request.getDeviceId(), ip, userAgent);

        authAuditLogRepository.save(AuthAuditLog.loginSuccess(user.getId(), email, ip, userAgent));
        log.info("Login successful: {}", email);

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

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthTokenResponse refreshToken(String refreshTokenValue, HttpServletRequest httpRequest) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!token.isValid()) {
            // FIX CVM-001: if an already-used token is presented, revoke the entire family
            if (token.getUsed()) {
                log.warn("Replay attack detected — revoking all USER tokens for userId={}",
                        token.getUserId());
                refreshTokenRepository.revokeAllUserTokens(token.getUserId());
            }
            throw new BadRequestException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String       newAccess  = jwtUtil.generateAccessToken(user);
        RefreshToken newRefresh = createRefreshToken(user, token.getDeviceId(),
                IpAddressUtil.getClientIp(httpRequest), ua(httpRequest));

        token.rotate(newRefresh.getToken());
        refreshTokenRepository.save(token);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.TOKEN_REFRESHED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                .userAgent(ua(httpRequest))
                .build());

        return AuthTokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String refreshTokenValue, String accessToken,
                       HttpServletRequest httpRequest) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
            authAuditLogRepository.save(AuthAuditLog.builder()
                    .userId(token.getUserId())
                    .eventType(AuthAuditLog.EventType.LOGOUT)
                    .status(AuthAuditLog.Status.SUCCESS)
                    .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                    .userAgent(ua(httpRequest))
                    .build());
        });

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims   = jwtUtil.validateAccessToken(accessToken);
                long remainSecs = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                if (remainSecs > 0) {
                    tokenBlacklistService.blacklist(accessToken, remainSecs);
                }
            } catch (JwtException ex) {
                log.debug("Access token not blacklisted (invalid/expired): {}", ex.getMessage());
            }
        }
    }

    // ── Email verification ────────────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken vt = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (!vt.isValid()) {
            throw new BadRequestException("Verification link has expired or already been used. "
                    + "Please log in to receive a new one.");
        }

        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);

        vt.markAsUsed();
        emailVerificationTokenRepository.save(vt);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.EMAIL_VERIFIED)
                .status(AuthAuditLog.Status.SUCCESS)
                .build());

        log.info("Email verified for: {}", user.getEmail());
    }

    // ── Forgot / Reset password ───────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest httpRequest) {
        var userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email (silent): {}", email);
            return;
        }

        User   user  = userOpt.get();
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(passwordResetExpiryHours))
                .requestIpAddress(IpAddressUtil.getClientIp(httpRequest))
                .requestUserAgent(ua(httpRequest))
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_REQUESTED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                .userAgent(ua(httpRequest))
                .build());

        log.info("Password reset email sent for: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String token, String newPassword, HttpServletRequest httpRequest) {
        PasswordResetToken rt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset link"));

        if (!rt.isValid()) {
            throw new BadRequestException("Reset link has expired or already been used");
        }

        validatePasswordStrength(newPassword);

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.resetFailedAttempts();
        userRepository.save(user);

        rt.markAsUsed(IpAddressUtil.getClientIp(httpRequest), ua(httpRequest));
        passwordResetTokenRepository.save(rt);

        refreshTokenRepository.revokeAllUserTokens(user.getId());

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_SUCCESS)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                .userAgent(ua(httpRequest))
                .build());

        log.info("Password reset complete for: {}", user.getEmail());
    }

    // ── Change password (authenticated) ──────────────────────────────────────

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

        refreshTokenRepository.revokeAllUserTokens(userId);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_CHANGED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                .userAgent(ua(httpRequest))
                .build());

        log.info("Password changed for: {}", user.getEmail());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RefreshToken createRefreshToken(User user, String deviceId,
                                            String ip, String userAgent) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .subjectType("USER")
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .deviceId(deviceId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }

    private void createAndSendEmailVerificationToken(User user, HttpServletRequest httpRequest) {
        // Delete all previous tokens for this user — only one active link at a time
        emailVerificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken vt = EmailVerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(emailVerificationExpiryHours))
                .requestIpAddress(IpAddressUtil.getClientIp(httpRequest))
                .requestUserAgent(ua(httpRequest))
                .build();

        emailVerificationTokenRepository.save(vt);
        emailService.sendVerificationEmail(user.getEmail(), token);

        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.EMAIL_VERIFICATION_SENT)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(httpRequest))
                .userAgent(ua(httpRequest))
                .build());

        log.info("Verification email sent to: {}", user.getEmail());
    }

    private void handleFailedLogin(User user, String ip, String userAgent) {
        user.incrementFailedAttempts();
        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lockAccount();
            log.warn("Account locked: {}", user.getEmail());
            authAuditLogRepository.save(AuthAuditLog.builder()
                    .userId(user.getId()).email(user.getEmail())
                    .eventType(AuthAuditLog.EventType.ACCOUNT_LOCKED)
                    .status(AuthAuditLog.Status.BLOCKED)
                    .ipAddress(ip).userAgent(userAgent)
                    .details("Max login attempts exceeded")
                    .build());
        }
        userRepository.save(user);
        authAuditLogRepository.save(
                AuthAuditLog.loginFailed(user.getEmail(), ip, userAgent, "Invalid password"));
    }

    private boolean shouldUnlock(User user) {
        if (user.getLockedAt() == null) return false;
        if (accountLockDurationMinutes == null || accountLockDurationMinutes <= 0) return false;
        return user.getLockedAt().plusMinutes(accountLockDurationMinutes)
                .isBefore(LocalDateTime.now());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8)
            throw new BadRequestException("Password must be at least 8 characters");
        if (!password.chars().anyMatch(Character::isUpperCase))
            throw new BadRequestException("Password must contain at least one uppercase letter");
        if (!password.chars().anyMatch(Character::isLowerCase))
            throw new BadRequestException("Password must contain at least one lowercase letter");
        if (!password.chars().anyMatch(Character::isDigit))
            throw new BadRequestException("Password must contain at least one digit");
        if (!password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0))
            throw new BadRequestException("Password must contain at least one special character");
    }

    public UserResponse mapToUserResponse(User user) {
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

    private boolean isTestProfile() {
        String[] active = environment.getActiveProfiles();
        return active.length > 0 && Arrays.asList(active).contains("test");
    }

    private String ua(HttpServletRequest r) { return r.getHeader("User-Agent"); }
}