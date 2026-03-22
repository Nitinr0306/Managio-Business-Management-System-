package com.nitin.saas.member.service;

import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.listener.MemberRegisteredEvent;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final AuthAuditLogRepository authAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

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

        Optional<Member> existingOpt =
                memberRepository.findByBusinessIdAndEmail(request.getBusinessId(), email);

        Member member;

        if (existingOpt.isPresent()) {
            member = existingOpt.get();

            if (member.getPassword() != null && Boolean.TRUE.equals(member.getEmailVerified())) {
                throw new ConflictException("EMAIL_ALREADY_REGISTERED");
            }

            member.setPassword(passwordEncoder.encode(request.getPassword()));
            member.setEmailVerified(false);

        } else {
            member = Member.builder()
                    .businessId(request.getBusinessId())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .status("ACTIVE")
                    .accountEnabled(true)
                    .emailVerified(false)
                    .build();
        }

        member = memberRepository.save(member);

        // 🔴 DELETE OLD TOKENS
        verificationRepo.deleteAllByMemberId(member.getId());

        // CREATE NEW TOKEN
        String token = UUID.randomUUID().toString();

        MemberEmailVerificationToken saved = verificationRepo.save(
                MemberEmailVerificationToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .used(false)
                        .build()
        );

        // ✅ EVENT-DRIVEN EMAIL
        eventPublisher.publishEvent(new MemberRegisteredEvent(member, saved.getToken()));

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

        if (member == null) throw new BadCredentialsException("INVALID_CREDENTIALS");
        if (!member.getAccountEnabled()) throw new BadCredentialsException("ACCOUNT_DISABLED");
        if (!"ACTIVE".equals(member.getStatus())) throw new BadCredentialsException("MEMBERSHIP_INACTIVE");

        if (!Boolean.TRUE.equals(member.getEmailVerified())) {
            throw new BusinessException("Email not verified", ErrorCode.EMAIL_NOT_VERIFIED);
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

        MemberEmailVerificationToken saved = verificationRepo.save(
                MemberEmailVerificationToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .used(false)
                        .build()
        );

        eventPublisher.publishEvent(new MemberRegisteredEvent(member, saved.getToken()));
    }

    // ================= FORGOT PASSWORD =================
    @Transactional
    public void requestPasswordReset(String identifier) {

        Member member = findMemberByIdentifier(identifier);
        if (member == null || member.getEmail() == null) return;

        String token = UUID.randomUUID().toString();

        resetRepo.save(
                MemberPasswordResetToken.builder()
                        .token(token)
                        .memberId(member.getId())
                        .expiresAt(LocalDateTime.now().plusHours(passwordResetExpiryHours))
                        .build()
        );

        // password reset email can remain direct
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