package com.nitin.saas.common.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    public void sendVerificationEmail(String email, String token) {
        log.info("Sending verification email to: {}", email);
        // TODO: Implement actual email sending (SendGrid, AWS SES, etc.)
        log.info("Verification link: /verify-email?token={}", token);
    }

    public void sendPasswordResetEmail(String email, String token) {
        log.info("Sending password reset email to: {}", email);
        // TODO: Implement actual email sending
        log.info("Reset link: /reset-password?token={}", token);
    }

    public void sendWelcomeEmail(String email, String name) {
        log.info("Sending welcome email to: {} ({})", name, email);
        // TODO: Implement actual email sending
    }

    public void sendSubscriptionExpiryReminder(String email, String memberName, int daysRemaining) {
        log.info("Sending subscription expiry reminder to: {} ({} days remaining)", email, daysRemaining);
        // TODO: Implement actual email sending
    }

    public void sendPaymentConfirmation(String email, String amount) {
        log.info("Sending payment confirmation to: {} (Amount: {})", email, amount);
        // TODO: Implement actual email sending
    }
}