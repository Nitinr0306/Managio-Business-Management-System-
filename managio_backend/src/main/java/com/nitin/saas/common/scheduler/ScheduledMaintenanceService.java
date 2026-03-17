package com.nitin.saas.common.scheduler;

import com.nitin.saas.auth.repository.EmailVerificationTokenRepository;
import com.nitin.saas.auth.repository.PasswordResetTokenRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.member.repository.MemberPasswordResetTokenRepository;
import com.nitin.saas.staff.repository.StaffInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled cleanup for all token tables.
 *
 * Tokens are never deleted immediately — a 1-day safety margin prevents
 * deleting tokens that are still in-flight due to propagation delays.
 *
 * Schedule overview:
 *   02:00  — expired refresh tokens
 *   02:15  — expired email verification tokens
 *   02:30  — expired user password reset tokens
 *   02:45  — expired member password reset tokens  (NEW)
 *   03:00  — expired staff invitations (30-day grace window)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledMaintenanceService {

    private final RefreshTokenRepository            refreshTokenRepository;
    private final EmailVerificationTokenRepository  emailVerificationTokenRepository;
    private final PasswordResetTokenRepository      passwordResetTokenRepository;
    private final MemberPasswordResetTokenRepository memberPasswordResetTokenRepository;
    private final StaffInvitationRepository         staffInvitationRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        refreshTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up refresh tokens expired before {}", cutoff);
    }

    @Scheduled(cron = "0 15 2 * * *")
    @Transactional
    public void cleanupExpiredEmailVerificationTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        emailVerificationTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up email verification tokens expired before {}", cutoff);
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        passwordResetTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up user password-reset tokens expired before {}", cutoff);
    }

    /** NEW: cleans up member password reset tokens. */
    @Scheduled(cron = "0 45 2 * * *")
    @Transactional
    public void cleanupExpiredMemberPasswordResetTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        memberPasswordResetTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up member password-reset tokens expired before {}", cutoff);
    }

    /**
     * Staff invitations — 30-day grace window so owners can audit who was invited
     * even after the invitation expired.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void cleanupExpiredStaffInvitations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        staffInvitationRepository.deleteExpiredInvitations(cutoff);
        log.info("Cleaned up staff invitations expired before {}", cutoff);
    }
}