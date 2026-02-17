package com.nitin.saas.member.service;

import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ConflictException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.member.dto.ChangePasswordRequest;
import com.nitin.saas.member.dto.MemberLoginRequest;
import com.nitin.saas.member.dto.MemberLoginResponse;
import com.nitin.saas.member.dto.MemberRegistrationRequest;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final BusinessRepository businessRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Transactional
    public MemberLoginResponse memberRegister(MemberRegistrationRequest request,
                                              HttpServletRequest httpRequest) {
        log.info("Member registration attempt: phone={}, businessId={}",
                request.getPhone(), request.getBusinessId());

        // Validate business exists
        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Check if phone already registered in this business
        Member existingMember = memberRepository.findByBusinessIdAndPhone(
                request.getBusinessId(), request.getPhone());

        if (existingMember != null) {
            throw new ConflictException("Phone number already registered in this business");
        }

        // Check if email already registered (if provided)
        if (request.getEmail() != null) {
            Member existingByEmail = memberRepository.findByBusinessIdAndEmail(
                    request.getBusinessId(), request.getEmail());
            if (existingByEmail != null) {
                throw new ConflictException("Email already registered in this business");
            }
        }

        // Create member
        Member member = Member.builder()
                .businessId(request.getBusinessId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .status("ACTIVE")
                .accountEnabled(true)
                .build();

        member = memberRepository.save(member);

        // Update business member count
        business.incrementMemberCount();
        businessRepository.save(business);

        // Log registration
        logMemberEvent(member.getId(), request.getPhone(), AuthAuditLog.EventType.REGISTER,
                AuthAuditLog.Status.SUCCESS, getClientIp(httpRequest), getUserAgent(httpRequest),
                "Member self-registered");

        log.info("Member registered successfully: id={}, businessId={}",
                member.getId(), request.getBusinessId());

        // Auto-login after registration
        return performMemberLogin(member, business, httpRequest);
    }

    @Transactional
    public MemberLoginResponse memberLogin(MemberLoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.getIdentifier();
        String ipAddress = getClientIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);

        log.info("Member login attempt: identifier={}, ip={}", identifier, ipAddress);

        // Find member by phone or email
        Member member = findMemberByIdentifier(identifier);

        if (member == null) {
            logMemberEvent(null, identifier, AuthAuditLog.EventType.LOGIN_FAILED,
                    AuthAuditLog.Status.FAILURE, ipAddress, userAgent, "Member not found");
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check if member account is enabled
        if (!member.getAccountEnabled()) {
            logMemberEvent(member.getId(), identifier, AuthAuditLog.EventType.LOGIN_FAILED,
                    AuthAuditLog.Status.BLOCKED, ipAddress, userAgent, "Account disabled");
            throw new BadCredentialsException("Your account has been disabled");
        }

        // Check if member is active
        if (!"ACTIVE".equals(member.getStatus())) {
            logMemberEvent(member.getId(), identifier, AuthAuditLog.EventType.LOGIN_FAILED,
                    AuthAuditLog.Status.BLOCKED, ipAddress, userAgent, "Member not active");
            throw new BadCredentialsException("Your membership is not active");
        }

        // Validate password
        if (member.getPassword() == null ||
                !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            logMemberEvent(member.getId(), identifier, AuthAuditLog.EventType.LOGIN_FAILED,
                    AuthAuditLog.Status.FAILURE, ipAddress, userAgent, "Invalid password");
            throw new BadCredentialsException("Invalid credentials");
        }

        // Get business
        Business business = businessRepository.findById(member.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Update last login
        member.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);

        // Log successful login
        logMemberEvent(member.getId(), identifier, AuthAuditLog.EventType.LOGIN_SUCCESS,
                AuthAuditLog.Status.SUCCESS, ipAddress, userAgent, "Member login successful");

        log.info("Member login successful: memberId={}, businessId={}",
                member.getId(), member.getBusinessId());

        return performMemberLogin(member, business, httpRequest);
    }

    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Update password
        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        memberRepository.save(member);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(memberId);

        log.info("Member password changed: memberId={}", memberId);
    }

    @Transactional
    public void requestPasswordReset(String identifier, HttpServletRequest httpRequest) {
        Member member = findMemberByIdentifier(identifier);

        if (member == null) {
            // Don't reveal if member exists
            log.info("Password reset requested for unknown identifier: {}", identifier);
            return;
        }

        // TODO: Generate reset token and send via SMS/Email
        // For now, just log it
        String resetToken = UUID.randomUUID().toString();
        log.info("Password reset token for member {}: {}", member.getId(), resetToken);

        logMemberEvent(member.getId(), identifier, AuthAuditLog.EventType.PASSWORD_RESET_REQUESTED,
                AuthAuditLog.Status.SUCCESS, getClientIp(httpRequest), getUserAgent(httpRequest),
                "Password reset requested");
    }

    @Transactional
    public void disableMemberAccount(Long memberId) {
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        member.setAccountEnabled(false);
        memberRepository.save(member);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(memberId);

        log.info("Member account disabled: memberId={}", memberId);
    }

    @Transactional
    public void enableMemberAccount(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        member.setAccountEnabled(true);
        if (member.getStatus().equals("INACTIVE")) {
            member.setStatus("ACTIVE");
        }
        memberRepository.save(member);

        log.info("Member account enabled: memberId={}", memberId);
    }

    private MemberLoginResponse performMemberLogin(Member member, Business business,
                                                   HttpServletRequest httpRequest) {
        // Generate JWT token for member
        String accessToken = jwtUtil.generateMemberAccessToken(member);

        // Create refresh token
        RefreshToken refreshToken = createRefreshToken(member.getId(),
                getClientIp(httpRequest), getUserAgent(httpRequest));

        // Get active subscription if any
        MemberLoginResponse.SubscriptionInfo subscriptionInfo = getSubscriptionInfo(member.getId());

        return MemberLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .member(mapToMemberInfo(member))
                .activeSubscription(subscriptionInfo)
                .business(mapToBusinessInfo(business))
                .lastLoginAt(member.getLastLoginAt())
                .build();
    }

    private Member findMemberByIdentifier(String identifier) {
        // Try to find by phone first
        if (identifier.matches("^[0-9+\\-\\s()]+$")) {
            // Looks like a phone number
            return memberRepository.findByPhone(identifier);
        }

        // Try by email
        if (identifier.contains("@")) {
            return memberRepository.findByEmail(identifier);
        }

        // Try both
        Member member = memberRepository.findByPhone(identifier);
        if (member == null) {
            member = memberRepository.findByEmail(identifier);
        }

        return member;
    }

    private MemberLoginResponse.SubscriptionInfo getSubscriptionInfo(Long memberId) {
        return subscriptionRepository.findActiveSubscriptionByMemberId(memberId)
                .map(sub -> {
                    SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
                    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());

                    return MemberLoginResponse.SubscriptionInfo.builder()
                            .subscriptionId(sub.getId())
                            .planName(plan != null ? plan.getName() : "Plan #" + sub.getPlanId())
                            .startDate(sub.getStartDate())
                            .endDate(sub.getEndDate())
                            .status(sub.getStatus())
                            .daysRemaining((int) daysRemaining)
                            .amountPaid(sub.getAmount())
                            .build();
                })
                .orElse(null);
    }

    private RefreshToken createRefreshToken(Long memberId, String ipAddress, String userAgent) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(memberId)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private MemberLoginResponse.MemberInfo mapToMemberInfo(Member member) {
        return MemberLoginResponse.MemberInfo.builder()
                .id(member.getId())
                .businessId(member.getBusinessId())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .fullName(member.getFullName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .dateOfBirth(member.getDateOfBirth())
                .gender(member.getGender())
                .status(member.getStatus())
                .memberSince(member.getCreatedAt())
                .build();
    }

    private MemberLoginResponse.BusinessInfo mapToBusinessInfo(Business business) {
        return MemberLoginResponse.BusinessInfo.builder()
                .id(business.getId())
                .name(business.getName())
                .address(business.getAddress())
                .phone(business.getPhone())
                .email(business.getEmail())
                .build();
    }

    private void logMemberEvent(Long memberId, String identifier, AuthAuditLog.EventType eventType,
                                AuthAuditLog.Status status, String ipAddress, String userAgent,
                                String details) {
        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(memberId)
                .email(identifier)
                .eventType(eventType)
                .status(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .build());
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