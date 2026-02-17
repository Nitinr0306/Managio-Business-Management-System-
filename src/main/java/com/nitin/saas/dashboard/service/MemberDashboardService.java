package com.nitin.saas.dashboard.service;

import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.dashboard.dto.*;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDashboardService {

    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;

    @Transactional(readOnly = true)
    public MemberDashboardResponse getMemberDashboard(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        MemberSubscription activeSub = subscriptionRepository
                .findActiveSubscriptionByMemberId(memberId)
                .orElse(null);

        String planName = "No Active Plan";
        LocalDate endDate = null;
        Integer daysRemaining = null;
        String status = "INACTIVE";

        if (activeSub != null) {
            SubscriptionPlan plan = planRepository.findById(activeSub.getPlanId()).orElse(null);
            planName = plan != null ? plan.getName() : "Plan #" + activeSub.getPlanId();
            endDate = activeSub.getEndDate();
            daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), endDate);
            status = activeSub.getStatus();
        }

        // Payment History
        List<Payment> payments = paymentRepository.findByMemberId(memberId);
        List<PaymentHistory> paymentHistory = payments.stream()
                .map(this::mapToPaymentHistory)
                .collect(Collectors.toList());

        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Membership Stats
        List<MemberSubscription> allSubscriptions = subscriptionRepository.findByMemberId(memberId);
        MembershipStats membershipStats = MembershipStats.builder()
                .totalSubscriptions(allSubscriptions.size())
                .completedSubscriptions((int) allSubscriptions.stream()
                        .filter(sub -> "EXPIRED".equals(sub.getStatus()))
                        .count())
                .totalSpent(totalPaid)
                .memberSince(member.getCreatedAt().toLocalDate())
                .build();

        return MemberDashboardResponse.builder()
                .memberName(member.getFullName())
                .planName(planName)
                .subscriptionEndDate(endDate)
                .daysRemaining(daysRemaining)
                .status(status)
                .paymentHistory(paymentHistory)
                .totalPaid(totalPaid)
                .membershipStats(membershipStats)
                .build();
    }

    private PaymentHistory mapToPaymentHistory(Payment payment) {
        return PaymentHistory.builder()
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().getDisplayName())
                .paidAt(payment.getCreatedAt().toLocalDate())
                .notes(payment.getNotes())
                .build();
    }
}