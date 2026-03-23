package com.nitin.saas.member.service;

import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.MemberPrincipal;
import com.nitin.saas.member.dto.MemberDetailResponse;
import com.nitin.saas.member.dto.MemberListItemResponse;
import com.nitin.saas.member.dto.SubscriptionHistoryResponse;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.dto.PaymentResponse;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberProfileService {

    private final MemberRepository             memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository   planRepository;
    private final PaymentRepository            paymentRepository;
    private final BusinessService              businessService;
    private final RBACService                  rbacService;

    // ── Member detail ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
        public MemberDetailResponse getMemberProfile(String memberIdentifier) {
                Member member = resolveMember(memberIdentifier);
                Long memberId = member.getId();

        if (rbacService.isMemberPrincipal()) {
            MemberPrincipal mp = rbacService.getCurrentMember();
            if (mp == null || !mp.getMemberId().equals(memberId)) {
                throw new AccessDeniedException("Members may only view their own profile");
            }
        } else {
            businessService.requireAccess(member.getBusinessId());
        }

        MemberDetailResponse.ActiveSubscriptionInfo activeSubInfo = getActiveSubscriptionInfo(memberId);
        List<PaymentResponse> paymentHistory = getPaymentHistory(memberId);

        BigDecimal totalPaid = paymentHistory.stream()
                .map(PaymentResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MemberSubscription> allSubs = subscriptionRepository.findByMemberId(memberId);

        return MemberDetailResponse.builder()
                .id(member.getId())
                .businessId(member.getBusinessId())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .fullName(member.getFullName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .dateOfBirth(member.getDateOfBirth())
                .gender(member.getGender())
                .address(member.getAddress())
                .status(member.getStatus())
                .notes(member.getNotes())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .activeSubscription(activeSubInfo)
                .paymentHistory(paymentHistory)
                .totalPaid(totalPaid)
                .totalSubscriptions(allSubs.size())
                .memberSince(member.getCreatedAt())
                .build();
    }

    // ── Members list with subscription info ───────────────────────────────────

    /**
     * FIX N+1: uses {@code findActiveSubscriptionsByMemberIds} batch query
     * instead of calling findActiveSubscriptionByMemberId once per member.
     *
     * For a page of 20 members this reduces subscription queries from 20 → 1.
     */
    @Transactional(readOnly = true)
    public Page<MemberListItemResponse> getMembersWithSubscriptions(Long businessId,
                                                                    Pageable pageable) {
        businessService.requireAccess(businessId);

        Page<Member> memberPage = memberRepository.findActiveByBusinessId(businessId, pageable);
        List<Member> members    = memberPage.getContent();

        if (members.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable,
                    memberPage.getTotalElements());
        }

        Set<Long> memberIds = members.stream()
                .map(Member::getId).collect(Collectors.toSet());

        // Batch 1: one query for all active subscriptions in this page
        Map<Long, MemberSubscription> activeSubByMemberId =
                subscriptionRepository.findActiveSubscriptionsByMemberIds(memberIds).stream()
                        .collect(Collectors.toMap(MemberSubscription::getMemberId, Function.identity(),
                                (a, b) -> a));   // keep first if somehow duplicates exist

        // Batch 2: load all referenced plans in one query
        Set<Long> planIds = activeSubByMemberId.values().stream()
                .map(MemberSubscription::getPlanId).collect(Collectors.toSet());
        Map<Long, SubscriptionPlan> planById = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, p -> p));

        List<MemberListItemResponse> items = members.stream().map(member -> {
            MemberSubscription sub = activeSubByMemberId.get(member.getId());

            String    subStatus  = "NONE";
            String    planName   = null;
            LocalDate endDate    = null;
            Integer   daysLeft   = null;

            if (sub != null) {
                SubscriptionPlan plan = planById.get(sub.getPlanId());
                planName  = plan != null ? plan.getName() : "Plan #" + sub.getPlanId();
                endDate   = sub.getEndDate();
                daysLeft  = (int) ChronoUnit.DAYS.between(LocalDate.now(), endDate);
                subStatus = sub.isExpired() ? "EXPIRED" : "ACTIVE";
            }

            return MemberListItemResponse.builder()
                    .id(member.getId())
                    .fullName(member.getFullName())
                    .phone(member.getPhone())
                    .email(member.getEmail())
                    .status(member.getStatus())
                    .subscriptionStatus(subStatus)
                    .activePlanName(planName)
                    .subscriptionEndDate(endDate)
                    .daysRemaining(daysLeft)
                    .build();

        }).collect(Collectors.toList());

        return new PageImpl<>(items, pageable, memberPage.getTotalElements());
    }

    // ── Subscription history ──────────────────────────────────────────────────

    /**
     * FIX M2: batch-loads all plans in a single findAllById call.
     */
    @Transactional(readOnly = true)
        public List<SubscriptionHistoryResponse> getSubscriptionHistory(String memberIdentifier) {
                Member member = resolveMember(memberIdentifier);
                Long memberId = member.getId();

        if (rbacService.isMemberPrincipal()) {
            MemberPrincipal mp = rbacService.getCurrentMember();
            if (mp == null || !mp.getMemberId().equals(memberId)) {
                throw new AccessDeniedException("Members may only view their own subscription history");
            }
        } else {
            businessService.requireAccess(member.getBusinessId());
        }

        List<MemberSubscription> subs = subscriptionRepository.findByMemberId(memberId);
        if (subs.isEmpty()) return Collections.emptyList();

        Set<Long> planIds = subs.stream()
                .map(MemberSubscription::getPlanId).collect(Collectors.toSet());
        Map<Long, SubscriptionPlan> planById = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, p -> p));

        return subs.stream()
                .map(s -> mapToSubscriptionHistory(s, planById.get(s.getPlanId())))
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MemberDetailResponse.ActiveSubscriptionInfo getActiveSubscriptionInfo(Long memberId) {
        return subscriptionRepository.findActiveSubscriptionByMemberId(memberId)
                .map(sub -> {
                    SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
                    return MemberDetailResponse.ActiveSubscriptionInfo.builder()
                            .subscriptionId(sub.getId())
                            .planId(sub.getPlanId())
                            .planName(plan != null ? plan.getName() : "Plan #" + sub.getPlanId())
                            .startDate(sub.getStartDate())
                            .endDate(sub.getEndDate())
                            .status(sub.getStatus())
                            .daysRemaining((int) days)
                            .amount(sub.getAmount())
                            .build();
                }).orElse(null);
    }

    private List<PaymentResponse> getPaymentHistory(Long memberId) {
        return paymentRepository.findByMemberId(memberId).stream()
                .map(p -> PaymentResponse.builder()
                        .id(p.getId())
                        .memberId(p.getMemberId())
                        .subscriptionId(p.getSubscriptionId())
                        .amount(p.getAmount())
                        .paymentMethod(p.getPaymentMethod())
                        .paymentMethodDisplay(p.getPaymentMethod().getDisplayName())
                        .notes(p.getNotes())
                        .recordedBy(p.getRecordedBy())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

        private Member resolveMember(String memberIdentifier) {
                String value = memberIdentifier == null ? "" : memberIdentifier.trim().toUpperCase(Locale.ROOT);
                if (value.isBlank()) {
                        throw new BadRequestException("Member identifier is required");
                }

                if (value.startsWith("MBR-")) {
                        return memberRepository.findActiveByPublicId(value)
                                        .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberIdentifier));
                }

                try {
                        Long id = Long.valueOf(value);
                        return memberRepository.findActiveById(id)
                                        .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberIdentifier));
                } catch (NumberFormatException ex) {
                        throw new BadRequestException("Invalid member identifier format");
                }
        }

    private SubscriptionHistoryResponse mapToSubscriptionHistory(MemberSubscription sub,
                                                                 SubscriptionPlan plan) {
        int duration = (int) ChronoUnit.DAYS.between(sub.getStartDate(), sub.getEndDate());
        return SubscriptionHistoryResponse.builder()
                .id(sub.getId())
                .planId(sub.getPlanId())
                .planName(plan != null ? plan.getName() : "Plan #" + sub.getPlanId())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .amount(sub.getAmount())
                .durationDays(duration)
                .createdAt(sub.getCreatedAt())
                .build();
    }
}