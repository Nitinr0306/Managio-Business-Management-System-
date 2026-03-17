package com.nitin.saas.member.service;

import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ConflictException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.common.utils.IpAddressUtil;
import com.nitin.saas.member.dto.ChangePasswordRequest;
import com.nitin.saas.member.dto.MemberLoginRequest;
import com.nitin.saas.member.dto.MemberLoginResponse;
import com.nitin.saas.member.dto.MemberRegistrationRequest;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.entity.MemberPasswordResetToken;
import com.nitin.saas.member.repository.MemberPasswordResetTokenRepository;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAuthService {

    private final MemberRepository                   memberRepository;
    private final BusinessRepository                 businessRepository;
    private final MemberSubscriptionRepository       subscriptionRepository;
    private final SubscriptionPlanRepository         planRepository;
    private final RefreshTokenRepository             refreshTokenRepository;
    private final MemberPasswordResetTokenRepository memberPasswordResetTokenRepository;
    private final AuthAuditLogRepository             authAuditLogRepository;
    private final EmailNotificationService           emailService;
    private final PasswordEncoder                    passwordEncoder;
    private final JwtUtil                            jwtUtil;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Value("${app.security.password-reset-expiry-hours:1}")
    private Integer passwordResetExpiryHours;

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public MemberLoginResponse memberRegister(MemberRegistrationRequest request,
                                              HttpServletRequest httpRequest) {

        log.info("Member registration: phone={}, businessId={}",
                request.getPhone(), request.getBusinessId());

        Business business = businessRepository.findActiveById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business not found. Please verify the business ID."));

        // -------------------------------------------------
        // PHONE CHECK
        // -------------------------------------------------

        Optional<Member> existingByPhone =
                memberRepository.findByBusinessIdAndPhone(
                        request.getBusinessId(),
                        request.getPhone());

        if (existingByPhone.isPresent()) {

            Member member = existingByPhone.get();

            // If password already exists → member already registered
            if (member.getPassword() != null) {
                throw new ConflictException(
                        "A member with this phone number is already registered in this business.");
            }

            // Invited member → complete registration
            member.setFirstName(request.getFirstName());
            member.setLastName(request.getLastName());
            member.setEmail(request.getEmail() != null ? request.getEmail().toLowerCase() : null);
            member.setPassword(passwordEncoder.encode(request.getPassword()));
            member.setDateOfBirth(request.getDateOfBirth());
            member.setGender(request.getGender());
            member.setAddress(request.getAddress());
            member.setStatus("ACTIVE");
            member.setAccountEnabled(true);

            member = memberRepository.save(member);

            log.info("Invited member completed registration: id={}", member.getId());

            return buildLoginResponse(member, business, httpRequest);
        }

        // -------------------------------------------------
        // EMAIL CHECK
        // -------------------------------------------------

        if (request.getEmail() != null && !request.getEmail().isBlank()) {

            Optional<Member> existingByEmail =
                    memberRepository.findByBusinessIdAndEmail(
                            request.getBusinessId(),
                            request.getEmail().toLowerCase());

            if (existingByEmail.isPresent()) {

                Member member = existingByEmail.get();

                if (member.getPassword() != null) {
                    throw new ConflictException(
                            "A member with this email is already registered in this business.");
                }

                member.setFirstName(request.getFirstName());
                member.setLastName(request.getLastName());
                member.setPhone(request.getPhone());
                member.setPassword(passwordEncoder.encode(request.getPassword()));
                member.setDateOfBirth(request.getDateOfBirth());
                member.setGender(request.getGender());
                member.setAddress(request.getAddress());
                member.setStatus("ACTIVE");
                member.setAccountEnabled(true);

                member = memberRepository.save(member);

                log.info("Invited member completed registration via email: id={}", member.getId());

                return buildLoginResponse(member, business, httpRequest);
            }
        }

        // -------------------------------------------------
        // CREATE NEW MEMBER
        // -------------------------------------------------

        Member member = Member.builder()
                .businessId(request.getBusinessId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail() != null ? request.getEmail().toLowerCase() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .status("ACTIVE")
                .accountEnabled(true)
                .build();

        member = memberRepository.save(member);

        business.incrementMemberCount();
        businessRepository.save(business);

        if (member.getEmail() != null) {
            emailService.sendMemberWelcomeEmail(
                    member.getEmail(),
                    member.getBusinessId(),
                    member.getFullName(),
                    business.getName());
        }

        logMemberEvent(member.getId(), request.getPhone(),
                AuthAuditLog.EventType.REGISTER, AuthAuditLog.Status.SUCCESS,
                IpAddressUtil.getClientIp(httpRequest), ua(httpRequest),
                "Member self-registered");

        log.info("Member registered: id={}, businessId={}",
                member.getId(), request.getBusinessId());

        return buildLoginResponse(member, business, httpRequest);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public MemberLoginResponse memberLogin(MemberLoginRequest request,
                                           HttpServletRequest httpRequest) {
        String identifier = request.getIdentifier().trim();
        String ip         = IpAddressUtil.getClientIp(httpRequest);
        String userAgent  = ua(httpRequest);

        Member member = findMemberByIdentifier(identifier);

        if (member == null) {
            logMemberEvent(null, identifier,
                    AuthAuditLog.EventType.LOGIN_FAILED, AuthAuditLog.Status.FAILURE,
                    ip, userAgent, "Member not found");
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!member.getAccountEnabled()) {
            logMemberEvent(member.getId(), identifier,
                    AuthAuditLog.EventType.LOGIN_FAILED, AuthAuditLog.Status.BLOCKED,
                    ip, userAgent, "Account disabled");
            throw new BadCredentialsException(
                    "Your account has been disabled. Please contact the front desk.");
        }

        if (!"ACTIVE".equals(member.getStatus())) {
            logMemberEvent(member.getId(), identifier,
                    AuthAuditLog.EventType.LOGIN_FAILED, AuthAuditLog.Status.BLOCKED,
                    ip, userAgent, "Member status=" + member.getStatus());
            throw new BadCredentialsException(
                    "Your membership is not active. Please contact the business.");
        }

        if (member.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            logMemberEvent(member.getId(), identifier,
                    AuthAuditLog.EventType.LOGIN_FAILED, AuthAuditLog.Status.FAILURE,
                    ip, userAgent, "Invalid password");
            throw new BadCredentialsException("Invalid credentials");
        }

        Business business = businessRepository.findActiveById(member.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        member.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);

        logMemberEvent(member.getId(), identifier,
                AuthAuditLog.EventType.LOGIN_SUCCESS, AuthAuditLog.Status.SUCCESS,
                ip, userAgent, "Member login successful");

        return buildLoginResponse(member, business, httpRequest);
    }

    // ── Change password (authenticated) ──────────────────────────────────────

    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }
        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        memberRepository.save(member);
        refreshTokenRepository.revokeAllMemberTokens(memberId);
        log.info("Member password changed: memberId={}", memberId);
    }

    // ── Forgot / Reset password ───────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(String identifier, HttpServletRequest httpRequest) {
        Member member = findMemberByIdentifier(identifier);
        if (member == null) {
            log.info("Member password reset requested for unknown identifier (silent): {}", identifier);
            return;
        }
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            log.warn("Member {} has no email — cannot send password reset", member.getId());
            return;
        }

        // Rate-limit: max 3 unused requests in the last hour
        long recent = memberPasswordResetTokenRepository
                .countRecentByMemberId(member.getId(), LocalDateTime.now().minusHours(1));
        if (recent >= 3) {
            log.warn("Member password reset rate limit hit: memberId={}", member.getId());
            return;
        }

        String token = UUID.randomUUID().toString();
        MemberPasswordResetToken resetToken = MemberPasswordResetToken.builder()
                .token(token)
                .memberId(member.getId())
                .expiresAt(LocalDateTime.now().plusHours(passwordResetExpiryHours))
                .requestIpAddress(IpAddressUtil.getClientIp(httpRequest))
                .requestUserAgent(ua(httpRequest))
                .build();

        memberPasswordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(member.getEmail(), token);

        logMemberEvent(member.getId(), identifier,
                AuthAuditLog.EventType.PASSWORD_RESET_REQUESTED, AuthAuditLog.Status.SUCCESS,
                IpAddressUtil.getClientIp(httpRequest), ua(httpRequest),
                "Member password reset email sent");

        log.info("Member password reset email sent: memberId={}", member.getId());
    }

    @Transactional
    public void resetPassword(String token, String newPassword, HttpServletRequest httpRequest) {
        MemberPasswordResetToken resetToken = memberPasswordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link. Please request a new one."));

        if (!resetToken.isValid()) {
            throw new BadRequestException(
                    "This reset link has expired or already been used. Please request a new one.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        Member member = memberRepository.findActiveById(resetToken.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member account not found"));

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        resetToken.markAsUsed(IpAddressUtil.getClientIp(httpRequest), ua(httpRequest));
        memberPasswordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllMemberTokens(member.getId());

        logMemberEvent(member.getId(),
                member.getEmail() != null ? member.getEmail() : member.getPhone(),
                AuthAuditLog.EventType.PASSWORD_RESET_SUCCESS, AuthAuditLog.Status.SUCCESS,
                IpAddressUtil.getClientIp(httpRequest), ua(httpRequest),
                "Member password reset completed");

        log.info("Member password reset completed: memberId={}", member.getId());
    }

    // ── Account management ────────────────────────────────────────────────────

    @Transactional
    public void disableMemberAccount(Long memberId) {
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        member.setAccountEnabled(false);
        memberRepository.save(member);
        refreshTokenRepository.revokeAllMemberTokens(memberId);
        log.info("Member account disabled: memberId={}", memberId);
    }

    @Transactional
    public void enableMemberAccount(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        member.setAccountEnabled(true);
        if ("INACTIVE".equals(member.getStatus())) member.setStatus("ACTIVE");
        memberRepository.save(member);
        log.info("Member account enabled: memberId={}", memberId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MemberLoginResponse buildLoginResponse(Member member, Business business,
                                                   HttpServletRequest httpRequest) {
        String       accessToken  = jwtUtil.generateMemberAccessToken(member);
        RefreshToken refreshToken = createMemberRefreshToken(
                member.getId(),
                IpAddressUtil.getClientIp(httpRequest),
                ua(httpRequest));

        return MemberLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .member(mapToMemberInfo(member))
                .activeSubscription(getSubscriptionInfo(member.getId()))
                .business(mapToBusinessInfo(business))
                .lastLoginAt(member.getLastLoginAt())
                .build();
    }

    private Member findMemberByIdentifier(String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        identifier = identifier.trim();

        // Phone detection
        if (identifier.matches("^[0-9+\\-\\s()]{6,20}$")) {
            return memberRepository
                    .findByPhone(identifier)
                    .orElse(null);
        }

        // Email detection
        if (identifier.contains("@")) {
            return memberRepository
                    .findByEmail(identifier.toLowerCase())
                    .orElse(null);
        }

        // fallback search
        Member m = memberRepository.findByPhone(identifier).orElse(null);

        return m != null
                ? m
                : memberRepository.findByEmail(identifier.toLowerCase()).orElse(null);
    }

    private MemberLoginResponse.SubscriptionInfo getSubscriptionInfo(Long memberId) {
        return subscriptionRepository.findActiveSubscriptionByMemberId(memberId)
                .map(sub -> {
                    SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
                    return MemberLoginResponse.SubscriptionInfo.builder()
                            .subscriptionId(sub.getId())
                            .planName(plan != null ? plan.getName() : "Plan #" + sub.getPlanId())
                            .startDate(sub.getStartDate())
                            .endDate(sub.getEndDate())
                            .status(sub.getStatus())
                            .daysRemaining((int) days)
                            .amountPaid(sub.getAmount())
                            .build();
                }).orElse(null);
    }

    private RefreshToken createMemberRefreshToken(Long memberId, String ip, String ua) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(memberId)
                .subjectType("MEMBER")
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .ipAddress(ip)
                .userAgent(ua)
                .build());
    }

    private MemberLoginResponse.MemberInfo mapToMemberInfo(Member m) {
        return MemberLoginResponse.MemberInfo.builder()
                .id(m.getId()).businessId(m.getBusinessId())
                .firstName(m.getFirstName()).lastName(m.getLastName())
                .fullName(m.getFullName()).phone(m.getPhone()).email(m.getEmail())
                .dateOfBirth(m.getDateOfBirth()).gender(m.getGender())
                .status(m.getStatus()).memberSince(m.getCreatedAt())
                .build();
    }

    private MemberLoginResponse.BusinessInfo mapToBusinessInfo(Business b) {
        return MemberLoginResponse.BusinessInfo.builder()
                .id(b.getId()).name(b.getName())
                .address(b.getAddress()).phone(b.getPhone()).email(b.getEmail())
                .build();
    }

    private void logMemberEvent(Long memberId, String identifier,
                                AuthAuditLog.EventType type, AuthAuditLog.Status status,
                                String ip, String ua, String details) {
        authAuditLogRepository.save(AuthAuditLog.builder()
                .userId(memberId).email(identifier)
                .eventType(type).status(status)
                .ipAddress(ip).userAgent(ua).details(details)
                .build());
    }

    private String ua(HttpServletRequest r) { return r.getHeader("User-Agent"); }
}