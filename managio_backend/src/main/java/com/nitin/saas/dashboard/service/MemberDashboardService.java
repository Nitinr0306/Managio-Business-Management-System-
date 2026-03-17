package com.nitin.saas.dashboard.service;

import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.MemberPrincipal;
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
import org.springframework.security.access.AccessDeniedException;
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

    private final MemberRepository             memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository            paymentRepository;
    private final SubscriptionPlanRepository   planRepository;
    private final BusinessService              businessService;
    private final RBACService                  rbacService;

    @Transactional(readOnly = true)
    public MemberDashboardResponse getMemberDashboard(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // ── Access control ─────────────────────────────────────────────────────
        // FIX CVH-003: use RBACService.isMemberPrincipal() which reads the actual
        // SecurityContext principal type — NOT getAuthorities() string matching,
        // which was broken because member tokens do not carry ROLE_MEMBER in the old code.
        if (rbacService.isMemberPrincipal()) {
            // A member token may only view their own dashboard
            MemberPrincipal mp = rbacService.getCurrentMember();
            if (mp == null || !mp.getMemberId().equals(memberId)) {
                throw new AccessDeniedException("Members may only view their own dashboard");
            }
        } else {
            // Owner or staff token — must have access to this member's business
            businessService.requireAccess(member.getBusinessId());
        }

        // ── Subscription ───────────────────────────────────────────────────────
        MemberSubscription activeSub = subscriptionRepository
                .findActiveSubscriptionByMemberId(memberId).orElse(null);

        String    planName     = "No Active Plan";
        LocalDate endDate      = null;
        Integer   daysLeft     = null;
        String    status       = "INACTIVE";

        if (activeSub != null) {
            SubscriptionPlan plan = planRepository.findById(activeSub.getPlanId()).orElse(null);
            planName  = plan != null ? plan.getName() : "Plan #" + activeSub.getPlanId();
            endDate   = activeSub.getEndDate();
            daysLeft  = (int) ChronoUnit.DAYS.between(LocalDate.now(), endDate);
            status    = activeSub.getStatus();
        }

        // ── Payments ───────────────────────────────────────────────────────────
        List<Payment> payments = paymentRepository.findByMemberId(memberId);

        List<PaymentHistory> history = payments.stream()
                .map(p -> PaymentHistory.builder()
                        .paymentId(p.getId())
                        .amount(p.getAmount())
                        .paymentMethod(p.getPaymentMethod().getDisplayName())
                        .paidAt(p.getCreatedAt().toLocalDate())
                        .notes(p.getNotes())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Membership stats ───────────────────────────────────────────────────
        List<MemberSubscription> allSubs = subscriptionRepository.findByMemberId(memberId);

        MembershipStats membershipStats = MembershipStats.builder()
                .totalSubscriptions(allSubs.size())
                .completedSubscriptions((int) allSubs.stream()
                        .filter(s -> "EXPIRED".equals(s.getStatus())).count())
                .totalSpent(totalPaid)
                .memberSince(member.getCreatedAt().toLocalDate())
                .build();

        return MemberDashboardResponse.builder()
                .memberName(member.getFullName())
                .planName(planName)
                .subscriptionEndDate(endDate)
                .daysRemaining(daysLeft)
                .status(status)
                .paymentHistory(history)
                .totalPaid(totalPaid)
                .membershipStats(membershipStats)
                .build();
    }
}