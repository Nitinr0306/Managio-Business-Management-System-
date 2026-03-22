package com.nitin.saas.member.service;

import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
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
import com.nitin.saas.member.entity.MemberPasswordResetToken;
import com.nitin.saas.member.repository.MemberPasswordResetTokenRepository;
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
    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberPasswordResetTokenRepository tokenRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final EmailNotificationService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.security.refresh-token-expiry-days:30}")
    private Integer refreshTokenExpiryDays;

    @Value("${app.security.password-reset-expiry-hours:1}")
    private Integer passwordResetExpiryHours;

    // ================= REGISTER =================
    // ONLY SHOWING CHANGED METHODS (not repeating whole class unnecessarily)

    @Transactional
    public MemberRegisterResponse memberRegister(MemberRegistrationRequest request,
                                                 HttpServletRequest httpRequest) {

        Business business = businessRepository.findActiveById(request.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("BUSINESS_NOT_FOUND"));

        String email = request.getEmail() != null ? request.getEmail().toLowerCase() : null;

        if (email != null) {
            Optional<Member> existingOpt =
                    memberRepository.findByBusinessIdAndEmail(request.getBusinessId(), email);

            if (existingOpt.isPresent()) {
                Member existing = existingOpt.get();

                if (existing.getPassword() != null && Boolean.TRUE.equals(existing.getEmailVerified())) {
                    throw new ConflictException("EMAIL_ALREADY_REGISTERED");
                }

                populateMember(existing, request);
                existing.setEmailVerified(false);
                memberRepository.save(existing);

                sendVerification(existing);

                return MemberRegisterResponse.builder()
                        .requiresVerification(true)
                        .email(existing.getEmail())
                        .build();
            }
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

        business.incrementMemberCount();
        businessRepository.save(business);

        sendVerification(member);

        return MemberRegisterResponse.builder()
                .requiresVerification(true)
                .email(member.getEmail())
                .build();
    }

    // ================= LOGIN =================
    @Transactional
    public MemberLoginResponse memberLogin(MemberLoginRequest request,
                                           HttpServletRequest httpRequest) {

        Member member = findMemberByIdentifier(request.getIdentifier());

        if (member == null) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        if (!member.getAccountEnabled()) {
            throw new BadCredentialsException("ACCOUNT_DISABLED");
        }

        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BadCredentialsException("MEMBERSHIP_INACTIVE");
        }

        if (!Boolean.TRUE.equals(member.getEmailVerified())) {
            throw new BusinessException(
                    "Email not verified",
                    ErrorCode.EMAIL_NOT_VERIFIED
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        Business business = businessRepository.findActiveById(member.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        member.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);

        return buildLoginResponse(member, business, httpRequest);
    }

    // ================= VERIFY =================
    @Transactional
    public void verifyEmail(String token) {
        MemberPasswordResetToken t = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("INVALID_TOKEN"));

        if (!t.isValid()) {
            throw new BadRequestException("TOKEN_EXPIRED");
        }

        Member member = memberRepository.findById(t.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND"));

        member.setEmailVerified(true);
        memberRepository.save(member);

        t.markAsUsed(null, null);
        tokenRepository.save(t);
    }

    // ================= RESEND =================
    @Transactional
    public void resendVerification(String email) {
        Member member = memberRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new BadRequestException("INVALID_REQUEST"));

        if (Boolean.TRUE.equals(member.getEmailVerified())) return;

        sendVerification(member);
    }

    // ================= FORGOT PASSWORD =================
    @Transactional
    public void requestPasswordReset(String identifier) {
        Member member = findMemberByIdentifier(identifier);
        if (member == null || member.getEmail() == null) {
            // Silently return to prevent account enumeration
            log.info("Password reset requested for unknown identifier: {}", identifier);
            return;
        }

        String token = UUID.randomUUID().toString();
        tokenRepository.save(
                MemberPasswordResetToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(passwordResetExpiryHours))
                        .build()
        );

        emailService.sendPasswordResetEmail(member.getEmail(), token);
    }

    // ================= RESET PASSWORD =================
    @Transactional
    public void resetPassword(String token, String newPassword) {
        MemberPasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link."));

        if (!resetToken.isValid()) {
            throw new BadRequestException("This reset link has expired. Please request a new one.");
        }

        Member member = memberRepository.findById(resetToken.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND"));

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        resetToken.markAsUsed(null, null);
        tokenRepository.save(resetToken);

        log.info("Password reset successful for member: {}", member.getEmail());
    }

    // ================= HELPERS =================

    private void populateMember(Member member, MemberRegistrationRequest request) {
        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setPhone(request.getPhone());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setStatus("ACTIVE");
        member.setAccountEnabled(true);
    }

    private void sendVerification(Member member) {
        if (member.getEmail() == null) return;

        String token = UUID.randomUUID().toString();

        tokenRepository.save(
                MemberPasswordResetToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .build()
        );

        emailService.sendVerificationEmail(member.getEmail(), token);
    }

    private MemberLoginResponse buildLoginResponse(Member member, Business business,
                                                   HttpServletRequest request) {

        String accessToken = jwtUtil.generateMemberAccessToken(member);

        RefreshToken refreshToken = refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .userId(member.getId())
                        .subjectType("MEMBER")
                        .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                        .ipAddress(IpAddressUtil.getClientIp(request))
                        .userAgent(request.getHeader("User-Agent"))
                        .build()
        );

        return MemberLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiry())
                .member(mapToMemberInfo(member))
                .business(mapToBusinessInfo(business))
                .lastLoginAt(member.getLastLoginAt())
                .build();
    }

    private Member findMemberByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return memberRepository.findByEmail(identifier.toLowerCase()).orElse(null);
        }
        return memberRepository.findByPhone(identifier).orElse(null);
    }

    private MemberLoginResponse.MemberInfo mapToMemberInfo(Member m) {
        return MemberLoginResponse.MemberInfo.builder()
                .id(m.getId())
                .businessId(m.getBusinessId())
                .fullName(m.getFullName())
                .phone(m.getPhone())
                .email(m.getEmail())
                .status(m.getStatus())
                .build();
    }

    private MemberLoginResponse.BusinessInfo mapToBusinessInfo(Business b) {
        return MemberLoginResponse.BusinessInfo.builder()
                .id(b.getId())
                .name(b.getName())
                .build();
    }
}