package com.nitin.saas.member.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.dto.MemberDetailResponse;
import com.nitin.saas.member.dto.MemberListItemResponse;
import com.nitin.saas.member.dto.SubscriptionHistoryResponse;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.dto.PaymentResponse;
import com.nitin.saas.payment.entity.Payment;
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
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public MemberDetailResponse getMemberProfile(Long memberId) {
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        businessService.requireAccess(member.getBusinessId());

        // Get active subscription
        MemberDetailResponse.ActiveSubscriptionInfo activeSubInfo = getActiveSubscriptionInfo(memberId);

        // Get payment history
        List<PaymentResponse> paymentHistory = getPaymentHistory(memberId);

        // Calculate statistics
        BigDecimal totalPaid = paymentHistory.stream()
                .map(PaymentResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalSubscriptions = subscriptionRepository.findByMemberId(memberId).stream().count();

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
                .totalSubscriptions(totalSubscriptions.intValue())
                .memberSince(member.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MemberListItemResponse> getMembersWithSubscriptions(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);

        Page<Member> members = memberRepository.findActiveByBusinessId(businessId, pageable);

        List<MemberListItemResponse> items = members.getContent().stream()
                .map(this::mapToListItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, members.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionHistoryResponse> getSubscriptionHistory(Long memberId) {
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        businessService.requireAccess(member.getBusinessId());

        List<MemberSubscription> subscriptions = subscriptionRepository.findByMemberId(memberId);

        return subscriptions.stream()
                .map(this::mapToSubscriptionHistory)
                .collect(Collectors.toList());
    }

    private MemberDetailResponse.ActiveSubscriptionInfo getActiveSubscriptionInfo(Long memberId) {
        return subscriptionRepository.findActiveSubscriptionByMemberId(memberId)
                .map(sub -> {
                    SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
                    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());

                    return MemberDetailResponse.ActiveSubscriptionInfo.builder()
                            .subscriptionId(sub.getId())
                            .planId(sub.getPlanId())
                            .planName(plan != null ? plan.getName() : "Plan #" + sub.getPlanId())
                            .startDate(sub.getStartDate())
                            .endDate(sub.getEndDate())
                            .status(sub.getStatus())
                            .daysRemaining((int) daysRemaining)
                            .amount(sub.getAmount())
                            .build();
                })
                .orElse(null);
    }

    private List<PaymentResponse> getPaymentHistory(Long memberId) {
        List<Payment> payments = paymentRepository.findByMemberId(memberId);

        return payments.stream()
                .map(payment -> PaymentResponse.builder()
                        .id(payment.getId())
                        .memberId(payment.getMemberId())
                        .subscriptionId(payment.getSubscriptionId())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod())
                        .paymentMethodDisplay(payment.getPaymentMethod().getDisplayName())
                        .notes(payment.getNotes())
                        .recordedBy(payment.getRecordedBy())
                        .createdAt(payment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private MemberListItemResponse mapToListItem(Member member) {
        MemberSubscription activeSub = subscriptionRepository
                .findActiveSubscriptionByMemberId(member.getId())
                .orElse(null);

        String subscriptionStatus = "NONE";
        String activePlanName = null;
        LocalDate subscriptionEndDate = null;
        Integer daysRemaining = null;

        if (activeSub != null) {
            SubscriptionPlan plan = planRepository.findById(activeSub.getPlanId()).orElse(null);
            activePlanName = plan != null ? plan.getName() : "Plan #" + activeSub.getPlanId();
            subscriptionEndDate = activeSub.getEndDate();
            daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), activeSub.getEndDate());
            subscriptionStatus = activeSub.isExpired() ? "EXPIRED" : "ACTIVE";
        }

        return MemberListItemResponse.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .status(member.getStatus())
                .subscriptionStatus(subscriptionStatus)
                .activePlanName(activePlanName)
                .subscriptionEndDate(subscriptionEndDate)
                .daysRemaining(daysRemaining)
                .build();
    }

    private SubscriptionHistoryResponse mapToSubscriptionHistory(MemberSubscription sub) {
        SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        int durationDays = (int) ChronoUnit.DAYS.between(sub.getStartDate(), sub.getEndDate());

        return SubscriptionHistoryResponse.builder()
                .id(sub.getId())
                .planId(sub.getPlanId())
                .planName(plan != null ? plan.getName() : "Plan #" + sub.getPlanId())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .amount(sub.getAmount())
                .durationDays(durationDays)
                .createdAt(sub.getCreatedAt())
                .build();
    }
}