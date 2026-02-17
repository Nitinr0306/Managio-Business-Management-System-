package com.nitin.saas.subscription.scheduler;

import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryReminderScheduler {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final EmailNotificationService emailService;

    /**
     * Runs daily at 9:00 AM to send expiry reminders
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpiryReminders() {
        log.info("Starting expiry reminder scheduler");

        LocalDate today = LocalDate.now();

        // Send reminders for subscriptions expiring in 7 days
        sendRemindersForDays(today, 7);

        // Send reminders for subscriptions expiring in 3 days
        sendRemindersForDays(today, 3);

        // Send reminders for subscriptions expiring in 1 day
        sendRemindersForDays(today, 1);

        log.info("Expiry reminder scheduler completed");
    }

    private void sendRemindersForDays(LocalDate today, int days) {
        LocalDate targetDate = today.plusDays(days);

        List<MemberSubscription> expiringSubs = subscriptionRepository
                .findExpiringSubscriptions(targetDate, targetDate);

        log.info("Found {} subscriptions expiring in {} days", expiringSubs.size(), days);

        for (MemberSubscription subscription : expiringSubs) {
            try {
                Member member = memberRepository.findById(subscription.getMemberId()).orElse(null);

                if (member == null || member.getEmail() == null) {
                    continue;
                }

                emailService.sendSubscriptionExpiryReminder(
                        member.getEmail(),
                        member.getFullName(),
                        days
                );

                log.debug("Sent expiry reminder to {} for subscription ending in {} days",
                        member.getEmail(), days);
            } catch (Exception e) {
                log.error("Failed to send expiry reminder for subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }
        }
    }

    /**
     * Runs daily at 10:00 AM to send payment confirmations for yesterday's payments
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendPaymentConfirmations() {
        log.info("Payment confirmation scheduler - feature coming soon");
        // TODO: Implement payment confirmation emails
    }
}