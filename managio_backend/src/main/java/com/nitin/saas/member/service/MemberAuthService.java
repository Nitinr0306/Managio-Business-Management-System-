package com.nitin.saas.member.service;

import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.event.MemberRegisteredEvent;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.*;
import com.nitin.saas.common.security.JwtUtil;
import com.nitin.saas.common.utils.IpAddressUtil;
import com.nitin.saas.member.dto.*;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.entity.MemberEmailVerificationToken;
import com.nitin.saas.member.entity.MemberPasswordResetToken;
import com.nitin.saas.member.repository.MemberEmailVerificationTokenRepository;
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
import org.springframework.context.ApplicationEventPublisher;
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

    private final MemberRepository memberRepository;
    private final BusinessRepository businessRepository;
    private final MemberEmailVerificationTokenRepository verificationRepo;
    private final MemberPasswordResetTokenRepository resetRepo;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAuditLogRepository auditRepo;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailNotificationService emailService;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Value("${app.security.password-reset-expiry-hours:1}")
    private Integer passwordResetExpiryHours;

    // ================= REGISTER =================
    @Transactional
    public MemberRegisterResponse memberRegister(MemberRegistrationRequest request,
                                                 HttpServletRequest httpRequest) {

        Business business = businessRepository.findActiveById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("BUSINESS_NOT_FOUND"));

        String email = request.getEmail().toLowerCase();

        if (memberRepository.findByBusinessIdAndEmail(request.getBusinessId(), email).isPresent()) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED");
        }

        Member member = Member.builder()
                .businessId(request.getBusinessId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .accountEnabled(true)
                .emailVerified(false)
                .build();

        member = memberRepository.save(member);

        String token = UUID.randomUUID().toString();

        verificationRepo.save(
                MemberEmailVerificationToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .used(false)
                        .build()
        );

        eventPublisher.publishEvent(new MemberRegisteredEvent(member, token));

        return MemberRegisterResponse.builder()
                .requiresVerification(true)
                .email(member.getEmail())
                .build();
    }

    // ================= LOGIN =================
    @Transactional
    public MemberLoginResponse memberLogin(MemberLoginRequest request,
                                           HttpServletRequest httpRequest) {

        String ip = IpAddressUtil.getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");

        Member member = findMemberByIdentifier(request.getIdentifier());

        if (member == null) {
            logFailed(request.getIdentifier(), ip, ua, "Member not found");
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        if (!member.getAccountEnabled()) {
            logFailed(member.getEmail(), ip, ua, "Account disabled");
            throw new BadCredentialsException("ACCOUNT_DISABLED");
        }

        if (!"ACTIVE".equals(member.getStatus())) {
            logFailed(member.getEmail(), ip, ua, "Inactive membership");
            throw new BadCredentialsException("MEMBERSHIP_INACTIVE");
        }

        if (!Boolean.TRUE.equals(member.getEmailVerified())) {
            logFailed(member.getEmail(), ip, ua, "Email not verified");
            throw new BadCredentialsException("EMAIL_NOT_VERIFIED");
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            logFailed(member.getEmail(), ip, ua, "Invalid password");
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        Business business = businessRepository.findActiveById(member.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        member.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);

        auditRepo.save(AuthAuditLog.builder()
                .userId(member.getId())
                .email(member.getEmail())
                .eventType(AuthAuditLog.EventType.LOGIN_SUCCESS)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(ip)
                .userAgent(ua)
                .details("Member login")
                .build());

        return buildLoginResponse(member, business, httpRequest);
    }

    // ================= VERIFY EMAIL =================
    @Transactional
    public void verifyEmail(String token) {

        MemberEmailVerificationToken vt = verificationRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("INVALID_TOKEN"));

        if (!vt.isValid()) {
            throw new BadRequestException("TOKEN_EXPIRED");
        }

        Member member = memberRepository.findById(vt.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND"));

        member.setEmailVerified(true);
        memberRepository.save(member);

        vt.setUsed(true);
        verificationRepo.save(vt);
    }

    // ================= RESEND =================
    @Transactional
    public void resendVerification(String email) {

        Member member = memberRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new BadRequestException("INVALID_REQUEST"));

        if (Boolean.TRUE.equals(member.getEmailVerified())) return;

        verificationRepo.deleteAllByMemberId(member.getId());

        String token = UUID.randomUUID().toString();

        verificationRepo.save(
                MemberEmailVerificationToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .used(false)
                        .build()
        );

        eventPublisher.publishEvent(new MemberRegisteredEvent(member, token));
    }

    // ================= FORGOT PASSWORD =================
    @Transactional
    public void requestPasswordReset(String identifier,
                                     HttpServletRequest request) {

        Member member = findMemberByIdentifier(identifier);
        if (member == null) return;

        String token = UUID.randomUUID().toString();

        resetRepo.save(
                MemberPasswordResetToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(passwordResetExpiryHours))
                        .build()
        );

        emailService.sendPasswordResetEmail(member.getEmail(), token);

        auditRepo.save(AuthAuditLog.builder()
                .userId(member.getId())
                .email(member.getEmail())
                .eventType(AuthAuditLog.EventType.PASSWORD_RESET_REQUESTED)
                .status(AuthAuditLog.Status.SUCCESS)
                .ipAddress(IpAddressUtil.getClientIp(request))
                .build());
    }

    // ================= RESET PASSWORD =================
    @Transactional
    public void resetPassword(String token, String newPassword) {

        MemberPasswordResetToken resetToken = resetRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("INVALID_TOKEN"));

        if (!resetToken.isValid()) {
            throw new BadRequestException("TOKEN_EXPIRED");
        }

        Member member = memberRepository.findById(resetToken.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND"));

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        resetToken.markAsUsed(null, null);
        resetRepo.save(resetToken);
    }

    // ================= HELPERS =================

    private Member findMemberByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return memberRepository.findByEmail(identifier.toLowerCase()).orElse(null);
        }
        return memberRepository.findByPhone(identifier).orElse(null);
    }

    private void logFailed(String email, String ip, String ua, String reason) {
        auditRepo.save(AuthAuditLog.loginFailed(email, ip, ua, reason));
    }

    private MemberLoginResponse buildLoginResponse(Member member, Business business,
                                                   HttpServletRequest request) {

        String accessToken = jwtUtil.generateMemberAccessToken(member);

        RefreshToken refreshToken = refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .userId(member.getId())
                        .subjectType("USER")
                        .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                        .ipAddress(IpAddressUtil.getClientIp(request))
                        .userAgent(request.getHeader("User-Agent"))
                        .build()
        );

        MemberLoginResponse.SubscriptionInfo subscriptionInfo =
                buildActiveSubscriptionInfo(member.getId());

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

    private MemberLoginResponse.SubscriptionInfo buildActiveSubscriptionInfo(Long memberId) {
        return memberSubscriptionRepository.findActiveSubscriptionByMemberId(memberId)
                .map(sub -> {
                    SubscriptionPlan plan = subscriptionPlanRepository.findById(sub.getPlanId()).orElse(null);
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
                })
                .orElse(null);
    }

    private MemberLoginResponse.MemberInfo mapToMemberInfo(Member m) {
        return MemberLoginResponse.MemberInfo.builder()
                .id(m.getId())
                .businessId(m.getBusinessId())
                .firstName(m.getFirstName())
                .lastName(m.getLastName())
                .fullName(m.getFullName())
                .phone(m.getPhone())
                .email(m.getEmail())
                .status(m.getStatus())
                .memberSince(m.getCreatedAt())
                .build();
    }

    private MemberLoginResponse.BusinessInfo mapToBusinessInfo(Business b) {
        return MemberLoginResponse.BusinessInfo.builder()
                .id(b.getId())
                .name(b.getName())
                .build();
    }
}