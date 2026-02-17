package com.nitin.saas.dashboard.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.dashboard.dto.*;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffDashboardService {

    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public StaffDashboardResponse getStaffDashboard(Long businessId) {
        businessService.requireAccess(businessId);

        // Members added today (simplified)
        Long membersAddedToday = 0L;
        Long totalActiveMembers = memberRepository.countActiveMembers(businessId);

        // Expiring subscriptions this week
        List<MemberSubscription> expiringSubs = subscriptionRepository.findExpiringSubscriptions(
                LocalDate.now(), LocalDate.now().plusDays(7));

        List<ExpiringSubscription> expiringThisWeek = expiringSubs.stream()
                .map(this::mapToExpiringSubscription)
                .limit(10)
                .collect(Collectors.toList());

        // Recent payments (last 3 days)
        List<Payment> recentPaymentsList = paymentRepository.findRecentPayments(businessId,
                LocalDateTime.now().minusDays(3));

        List<RecentPayment> recentPayments = recentPaymentsList.stream()
                .map(this::mapToRecentPayment)
                .limit(10)
                .collect(Collectors.toList());

        // Task Reminders
        List<TaskReminder> taskReminders = generateTaskReminders(businessId, expiringSubs);

        return StaffDashboardResponse.builder()
                .membersAddedToday(membersAddedToday)
                .totalActiveMembers(totalActiveMembers)
                .expiringThisWeek(expiringThisWeek)
                .recentPayments(recentPayments)
                .taskReminders(taskReminders)
                .build();
    }

    private ExpiringSubscription mapToExpiringSubscription(MemberSubscription sub) {
        Member member = memberRepository.findById(sub.getMemberId()).orElse(null);
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());

        return ExpiringSubscription.builder()
                .memberId(sub.getMemberId())
                .memberName(member != null ? member.getFullName() : "Unknown")
                .planName("Plan #" + sub.getPlanId())
                .endDate(sub.getEndDate())
                .daysRemaining((int) daysRemaining)
                .phone(member != null ? member.getPhone() : null)
                .email(member != null ? member.getEmail() : null)
                .build();
    }

    private RecentPayment mapToRecentPayment(Payment payment) {
        Member member = memberRepository.findById(payment.getMemberId()).orElse(null);

        return RecentPayment.builder()
                .paymentId(payment.getId())
                .memberName(member != null ? member.getFullName() : "Unknown")
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().getDisplayName())
                .paidAt(payment.getCreatedAt().toLocalDate())
                .build();
    }

    private List<TaskReminder> generateTaskReminders(Long businessId, List<MemberSubscription> expiringSubs) {
        List<TaskReminder> reminders = new ArrayList<>();

        if (!expiringSubs.isEmpty()) {
            reminders.add(TaskReminder.builder()
                    .task("Follow up on expiring subscriptions")
                    .count(expiringSubs.size())
                    .priority("HIGH")
                    .build());
        }

        // Add more task reminders as needed
        return reminders;
    }
}