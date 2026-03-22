package com.nitin.saas.payment.service;

import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.dto.PaymentResponse;
import com.nitin.saas.payment.dto.RecordPaymentRequest;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import com.nitin.saas.auth.service.RBACService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository            paymentRepository;
    private final MemberRepository             memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository   planRepository;
    private final BusinessService              businessService;
    private final RBACService                  rbacService;
    private final EmailNotificationService     emailService;
    private final AuditLogService              auditLogService;

    // ── Record payment ────────────────────────────────────────────────────────

    /**
     * Records a manual payment for a member.
     *
     * FIX CVL-003: now sends a payment confirmation email to the member
     * when they have a registered email address.
     */
    @Transactional
    public PaymentResponse recordPayment(Long businessId, RecordPaymentRequest request) {
        businessService.requireAccess(businessId);

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found: " + request.getMemberId()));

        if (!member.getBusinessId().equals(businessId)) {
            throw new BadRequestException("Member does not belong to this business");
        }

        Long recordedBy = rbacService.getCurrentUserId();

        // Parse paidAt from the request (ISO date string like "2025-03-20")
        LocalDateTime paidAtTime = null;
        if (request.getPaidAt() != null && !request.getPaidAt().isBlank()) {
            try {
                paidAtTime = LocalDate.parse(request.getPaidAt()).atStartOfDay();
            } catch (Exception e) {
                log.warn("Invalid paidAt date format: {}, using current time", request.getPaidAt());
                paidAtTime = LocalDateTime.now();
            }
        }

        Payment payment = Payment.builder()
                .memberId(request.getMemberId())
                .subscriptionId(request.getSubscriptionId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .recordedBy(recordedBy)
                .paidAt(paidAtTime)
                .build();

        payment = paymentRepository.save(payment);

        // Activate the linked subscription if one was provided and it's not yet active
        if (request.getSubscriptionId() != null) {
            activateSubscription(request.getSubscriptionId());
        }

        // Audit log
        auditLogService.logManualPayment(
                businessId, payment.getId(),
                payment.getAmount().toPlainString(),
                payment.getPaymentMethod().getDisplayName());

        // FIX CVL-003: send confirmation email to member (async — fire-and-forget)
        if (member.getEmail() != null && !member.getEmail().isBlank()) {
            emailService.sendPaymentConfirmation(
                    member.getEmail(),
                    businessId,                       // ADD
                    member.getFullName(),
                    payment.getAmount().toPlainString(),
                    payment.getPaymentMethod().getDisplayName());
        }

        log.info("Payment recorded: paymentId={}, memberId={}, amount={}, method={}, by={}",
                payment.getId(), member.getId(),
                payment.getAmount(), payment.getPaymentMethod(), recordedBy);

        return mapToResponse(payment, member);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * FIX B7: batch-loads members via findAllById to avoid N+1.
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByBusiness(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);

        Page<Payment> paymentPage = paymentRepository.findByBusinessId(businessId, pageable);
        List<Payment> payments    = paymentPage.getContent();

        if (payments.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, paymentPage.getTotalElements());
        }

        Set<Long> memberIds = payments.stream()
                .map(Payment::getMemberId)
                .collect(Collectors.toSet());

        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        List<PaymentResponse> responses = payments.stream()
                .map(p -> mapToResponse(p, memberMap.get(p.getMemberId())))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, paymentPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMemberPaymentHistory(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        businessService.requireAccess(member.getBusinessId());
        return paymentRepository.findByMemberId(memberId).stream()
                .map(p -> mapToResponse(p, member))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRevenue(Long businessId, LocalDateTime since) {
        businessService.requireAccess(businessId);
        BigDecimal revenue = paymentRepository.calculateRevenue(businessId, since);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getRecentPayments(Long businessId, int days) {
        businessService.requireAccess(businessId);
        LocalDateTime since    = LocalDateTime.now().minusDays(days);
        List<Payment> payments = paymentRepository.findRecentPayments(businessId, since);

        if (payments.isEmpty()) return List.of();

        Set<Long> memberIds = payments.stream()
                .map(Payment::getMemberId)
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return payments.stream()
                .map(p -> mapToResponse(p, memberMap.get(p.getMemberId())))
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void activateSubscription(Long subscriptionId) {
        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            if (!"ACTIVE".equals(subscription.getStatus())) {
                subscription.setStatus("ACTIVE");
                subscriptionRepository.save(subscription);
                log.info("Subscription activated on payment: subscriptionId={}", subscriptionId);
            }
        });
    }

    private PaymentResponse mapToResponse(Payment payment, Member member) {
        // Look up plan name from subscription → plan if linked
        String planName = null;
        if (payment.getSubscriptionId() != null) {
            planName = subscriptionRepository.findById(payment.getSubscriptionId())
                    .map(sub -> planRepository.findById(sub.getPlanId())
                            .map(SubscriptionPlan::getName)
                            .orElse(null))
                    .orElse(null);
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .memberId(payment.getMemberId())
                .memberName(member != null ? member.getFullName() : "Unknown")
                .memberPhone(member != null ? member.getPhone() : null)
                .subscriptionId(payment.getSubscriptionId())
                .planName(planName)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentMethodDisplay(payment.getPaymentMethod() != null
                        ? payment.getPaymentMethod().getDisplayName() : null)
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .recordedBy(payment.getRecordedBy())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}