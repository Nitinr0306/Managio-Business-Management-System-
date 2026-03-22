package com.nitin.saas.auth.service;

import com.nitin.saas.auth.dto.*;
import com.nitin.saas.auth.entity.*;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.*;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.*;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.common.security.TokenBlacklistService;
import com.nitin.saas.common.utils.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailTokenRepo;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthAuditLogRepository auditRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailNotificationService emailService;
    private final TokenBlacklistService blacklistService;
    private final Environment environment;

    @Value("${app.security.email-verification-expiry-hours:24}")
    private Integer emailExpiry;

    @Value("${app.security.password-reset-expiry-hours:1}")
    private Integer resetExpiry;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshExpiry;

    @Value("${app.security.max-login-attempts:5}")
    private Integer maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private Integer accountLockDurationMinutes;

    // =========================================================
    // REGISTER
    // =========================================================
    @Transactional
    public UserResponse register(RegisterRequest req, HttpServletRequest request) {

        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                .email(req.getEmail().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhoneNumber())
                .roles(new HashSet<>(Set.of(Role.USER)))
                .emailVerified(false)
                .enabled(true)
                .accountLocked(false)
                .accountStatus(User.AccountStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);

        createAndSendVerification(user, request);

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");
        auditRepo.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.REGISTER)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .build());

        log.info("User registered: {}", user.getEmail());

        return mapToUserResponse(user);
    }

    // =========================================================
    // LOGIN
    // =========================================================
    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletRequest request) {

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    auditRepo.save(AuthAuditLog.loginFailed(req.getEmail(), ip, ua, "User not found"));
                    return new BusinessException("Invalid credentials", ErrorCode.INVALID_CREDENTIALS);
                });

        // Check account lock
        if (user.getAccountLocked()) {
            if (shouldUnlock(user)) {
                user.unlockAccount();
                userRepository.save(user);
                log.info("Account auto-unlocked: {}", user.getEmail());
            } else {
                auditRepo.save(AuthAuditLog.loginFailed(user.getEmail(), ip, ua, "Account locked"));
                throw new AccountLockedException("Account is locked. Please try again later.");
            }
        }

        if (!user.getEnabled()) {
            auditRepo.save(AuthAuditLog.loginFailed(user.getEmail(), ip, ua, "Account disabled"));
            throw new BusinessException("Account is disabled", ErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ip, ua);
            throw new BusinessException("Invalid credentials", ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.getEmailVerified()) {
            createAndSendVerification(user, request);
            throw new BusinessException(
                    "Email not verified. Verification email sent.",
                    ErrorCode.EMAIL_NOT_VERIFIED
            );
        }

        // Successful login — reset fail counters
        user.resetFailedAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String access = jwtUtil.generateAccessToken(user);
        RefreshToken refresh = createRefreshToken(user, request);

        auditRepo.save(AuthAuditLog.loginSuccess(user.getId(), user.getEmail(), ip, ua));

        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .user(mapToUserResponse(user))
                .requiresTwoFactor(false)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    // =========================================================
    // REFRESH TOKEN
    // =========================================================
    @Transactional
    public AuthTokenResponse refreshToken(String token, HttpServletRequest request) {

        RefreshToken existing = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", ErrorCode.INVALID_CREDENTIALS));

        if (!existing.isValid()) {
            // If the token has already been used, this may be a replay attack
            // Revoke all tokens for this user as a safety measure
            if (existing.getUsed()) {
                log.warn("Refresh token replay detected for userId={}", existing.getUserId());
                refreshTokenRepository.revokeAllUserTokens(existing.getUserId());
            }
            throw new BusinessException("Refresh token expired or revoked", ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEnabled() || user.getAccountLocked()) {
            throw new BusinessException("Account is disabled or locked", ErrorCode.INVALID_CREDENTIALS);
        }

        // Rotate: mark old token as used, create new one
        String newAccess = jwtUtil.generateAccessToken(user);
        RefreshToken newRefresh = createRefreshToken(user, request);

        existing.rotate(newRefresh.getToken());
        refreshTokenRepository.save(existing);

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");
        auditRepo.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.TOKEN_REFRESHED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .build());

        return AuthTokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .build();
    }

    // =========================================================
    // LOGOUT
    // =========================================================
    @Transactional
    public void logout(String refreshToken, String accessToken, HttpServletRequest request) {

        // Revoke refresh token
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
                rt.revoke();
                refreshTokenRepository.save(rt);
            });
        }

        // Blacklist access token so it can't be reused
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                blacklistService.blacklist(accessToken, jwtUtil.getAccessTokenExpiry());
            } catch (Exception e) {
                log.warn("Could not blacklist access token: {}", e.getMessage());
            }
        }

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");
        auditRepo.save(AuthAuditLog.builder()
                .eventType(AuthAuditLog.EventType.LOGOUT)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .details("Logout completed")
                .build());

        log.info("Logout completed from ip={}", ip);
    }

    // =========================================================
    // VERIFY EMAIL
    // =========================================================
    @Transactional
    public void verifyEmail(String token) {

        token = URLDecoder.decode(token, StandardCharsets.UTF_8);

        EmailVerificationToken vt = emailTokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification link"));

        if (!vt.isValid()) {
            throw new BadRequestException("Verification link expired or used");
        }

        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);

        vt.markAsUsed();
        emailTokenRepo.save(vt);

        log.info("Email verified: {}", user.getEmail());
    }

    // =========================================================
    // RESEND VERIFICATION
    // =========================================================
    @Transactional
    public void resendVerificationEmail(String email, HttpServletRequest request) {

        // Silent return for non-existent users (anti-enumeration)
        var userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        if (user.getEmailVerified()) return;

        // Invalidate all existing verification tokens before creating a new one
        emailTokenRepo.findByToken(email); // no-op query to warm cache
        createAndSendVerification(user, request);
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================
    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest request) {

        // Always return success to prevent email enumeration
        var userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        String token = UUID.randomUUID().toString();
        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        PasswordResetToken rt = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(resetExpiry))
                .requestIpAddress(ip)
                .requestUserAgent(ua)
                .build();

        passwordResetTokenRepository.save(rt);

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        auditRepo.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_REQUESTED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .build());
    }

    @Transactional
    public void resetPassword(String token, String newPassword, HttpServletRequest request) {

        token = URLDecoder.decode(token, StandardCharsets.UTF_8);

        PasswordResetToken rt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (!rt.isValid()) {
            throw new BadRequestException("Token expired");
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        rt.markAsUsed(ip, ua);
        passwordResetTokenRepository.save(rt);

        // Revoke all existing sessions on password reset
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        auditRepo.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_SUCCESS)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .build());

        log.info("Password reset completed for: {}", user.getEmail());
    }

    // =========================================================
    // CHANGE PASSWORD (authenticated)
    // =========================================================
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword,
                               HttpServletRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect", ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke all refresh tokens — forces re-login on all devices
        refreshTokenRepository.revokeAllUserTokens(userId);

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");
        auditRepo.save(AuthAuditLog.builder()
                .userId(user.getId()).email(user.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_CHANGED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip).userAgent(ua)
                .build());

        log.info("Password changed for: {}", user.getEmail());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void handleFailedLogin(User user, String ip, String ua) {
        user.incrementFailedAttempts();
        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lockAccount();
            auditRepo.save(AuthAuditLog.builder()
                    .userId(user.getId()).email(user.getEmail())
                    .eventType(AuthAuditLog.EventType.ACCOUNT_LOCKED)
                    .status(AuthAuditLog.Status.BLOCKED)
                    .ipAddress(ip).userAgent(ua)
                    .details("Max login attempts exceeded")
                    .build());
            log.warn("Account locked due to max login attempts: {}", user.getEmail());
        }
        userRepository.save(user);
        auditRepo.save(AuthAuditLog.loginFailed(user.getEmail(), ip, ua, "Invalid password"));
    }

    private boolean shouldUnlock(User user) {
        if (user.getLockedAt() == null) return false;
        return user.getLockedAt().plusMinutes(accountLockDurationMinutes)
                .isBefore(LocalDateTime.now());
    }

    private void createAndSendVerification(User user, HttpServletRequest request) {

        String token = UUID.randomUUID().toString();

        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        EmailVerificationToken vt = EmailVerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(emailExpiry))
                .requestIpAddress(ip)
                .requestUserAgent(ua)
                .build();

        emailTokenRepo.save(vt);

        emailService.sendVerificationEmail(user.getEmail(), token);

        log.debug("Verification email sent to: {}", user.getEmail());
    }

    private RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        String ip = IpAddressUtil.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        return refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .userId(user.getId())
                        .subjectType("USER")
                        .expiresAt(LocalDateTime.now().plusDays(refreshExpiry))
                        .ipAddress(ip)
                        .userAgent(ua)
                        .build()
        );
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
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}