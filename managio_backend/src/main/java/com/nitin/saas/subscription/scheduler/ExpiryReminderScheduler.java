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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryReminderScheduler {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final MemberRepository             memberRepository;
    private final EmailNotificationService     emailService;

    /** Runs daily at 09:00 to send expiry reminders. */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpiryReminders() {
        log.info("Starting expiry reminder scheduler");

        LocalDate today = LocalDate.now();
        sendRemindersForDays(today, 7);
        sendRemindersForDays(today, 3);
        sendRemindersForDays(today, 1);

        log.info("Expiry reminder scheduler completed");
    }

    /**
     * FIX H3: the original code called memberRepository.findById() inside the
     * loop — one query per subscription (N+1).
     *
     * The fix collects all memberIds first, batch-loads them in a single
     * findAllById() call, and then maps using an O(1) HashMap lookup.
     */
    private void sendRemindersForDays(LocalDate today, int days) {
        LocalDate targetDate = today.plusDays(days);

        List<MemberSubscription> expiringSubs = subscriptionRepository
                .findExpiringSubscriptions(targetDate, targetDate);

        if (expiringSubs.isEmpty()) {
            return;
        }

        log.info("Found {} subscriptions expiring in {} days", expiringSubs.size(), days);

        // Batch-load all required members in ONE query
        Set<Long> memberIds = expiringSubs.stream()
                .map(MemberSubscription::getMemberId)
                .collect(Collectors.toSet());

        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        for (MemberSubscription subscription : expiringSubs) {
            try {
                Member member = memberMap.get(subscription.getMemberId());

                if (member == null || member.getEmail() == null) {
                    continue;
                }

                emailService.sendSubscriptionExpiryReminder(
                        member.getEmail(),
                        member.getBusinessId(),      // ADD
                        member.getFullName(),
                        days);

                log.debug("Sent expiry reminder to {} for subscription ending in {} days",
                        member.getEmail(), days);

            } catch (Exception e) {
                log.error("Failed to send expiry reminder for subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }
        }
    }

    /** Runs daily at 10:00 — placeholder for future payment confirmation emails. */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendPaymentConfirmations() {
        log.info("Payment confirmation scheduler - feature coming soon");
    }
}