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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffDashboardService {

    private final MemberRepository             memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository            paymentRepository;
    private final BusinessService              businessService;

    @Transactional(readOnly = true)
    public StaffDashboardResponse getStaffDashboard(Long businessId) {
        businessService.requireAccess(businessId);

        Long totalActiveMembers = memberRepository.countActiveMembers(businessId);

        LocalDate today = LocalDate.now();
        List<MemberSubscription> expiringSubs = subscriptionRepository
                .findExpiringByBusinessId(businessId, today, today.plusDays(7));

        List<ExpiringSubscription> expiringThisWeek = buildExpiringSubscriptions(
                expiringSubs.stream().limit(10).collect(Collectors.toList()));

        List<Payment> recentPaymentEntities = paymentRepository
                .findRecentPayments(businessId, LocalDateTime.now().minusDays(3))
                .stream().limit(10).collect(Collectors.toList());

        List<RecentPayment> recentPayments = buildRecentPayments(recentPaymentEntities);
        List<TaskReminder> taskReminders   = generateTaskReminders(expiringSubs);

        return StaffDashboardResponse.builder()
                .membersAddedToday(0L)
                .totalActiveMembers(totalActiveMembers)
                .expiringThisWeek(expiringThisWeek)
                .recentPayments(recentPayments)
                .taskReminders(taskReminders)
                .build();
    }

    private List<ExpiringSubscription> buildExpiringSubscriptions(List<MemberSubscription> subs) {
        if (subs.isEmpty()) return Collections.emptyList();
        Set<Long> ids = subs.stream().map(MemberSubscription::getMemberId).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
        return subs.stream().map(sub -> {
            Member m = memberMap.get(sub.getMemberId());
            long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
            return ExpiringSubscription.builder()
                    .memberId(sub.getMemberId())
                    .memberName(m != null ? m.getFullName() : "Unknown")
                    .planName("Plan #" + sub.getPlanId())
                    .endDate(sub.getEndDate()).daysRemaining((int) days)
                    .phone(m != null ? m.getPhone() : null)
                    .email(m != null ? m.getEmail() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<RecentPayment> buildRecentPayments(List<Payment> payments) {
        if (payments.isEmpty()) return Collections.emptyList();
        Set<Long> ids = payments.stream().map(Payment::getMemberId).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
        return payments.stream().map(p -> {
            Member m = memberMap.get(p.getMemberId());
            return RecentPayment.builder()
                    .paymentId(p.getId())
                    .memberName(m != null ? m.getFullName() : "Unknown")
                    .amount(p.getAmount())
                    .paymentMethod(p.getPaymentMethod().getDisplayName())
                    .paidAt(p.getCreatedAt().toLocalDate())
                    .build();
        }).collect(Collectors.toList());
    }

    private List<TaskReminder> generateTaskReminders(List<MemberSubscription> expiringSubs) {
        List<TaskReminder> reminders = new ArrayList<>();
        if (!expiringSubs.isEmpty()) {
            reminders.add(TaskReminder.builder()
                    .task("Follow up on expiring subscriptions")
                    .count(expiringSubs.size()).priority("HIGH").build());
        }
        return reminders;
    }
}