package com.nitin.saas.payment.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.dto.PaymentResponse;
import com.nitin.saas.payment.dto.RecordPaymentRequest;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.auth.service.RBACService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final BusinessService businessService;
    private final RBACService rbacService;

    @Transactional
    public PaymentResponse recordPayment(Long businessId, RecordPaymentRequest request) {
        businessService.requireAccess(businessId);

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!member.getBusinessId().equals(businessId)) {
            throw new BadRequestException("Member does not belong to this business");
        }

        Long recordedBy = rbacService.getCurrentUserId();

        Payment payment = Payment.builder()
                .memberId(request.getMemberId())
                .subscriptionId(request.getSubscriptionId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .recordedBy(recordedBy)
                .build();

        payment = paymentRepository.save(payment);

        if (request.getSubscriptionId() != null) {
            activateSubscription(request.getSubscriptionId());
        }

        log.info("Payment recorded: {} for member: {} by user: {}",
                payment.getId(), member.getId(), recordedBy);

        return mapToResponse(payment, member);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByBusiness(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);
        return paymentRepository.findByBusinessId(businessId, pageable)
                .map(payment -> {
                    Member member = memberRepository.findById(payment.getMemberId()).orElse(null);
                    return mapToResponse(payment, member);
                });
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMemberPaymentHistory(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        businessService.requireAccess(member.getBusinessId());

        return paymentRepository.findByMemberId(memberId).stream()
                .map(payment -> mapToResponse(payment, member))
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
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return paymentRepository.findRecentPayments(businessId, since).stream()
                .map(payment -> {
                    Member member = memberRepository.findById(payment.getMemberId()).orElse(null);
                    return mapToResponse(payment, member);
                })
                .collect(Collectors.toList());
    }

    private void activateSubscription(Long subscriptionId) {
        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            if (!"ACTIVE".equals(subscription.getStatus())) {
                subscription.setStatus("ACTIVE");
                subscriptionRepository.save(subscription);
                log.info("Subscription activated: {}", subscriptionId);
            }
        });
    }

    private PaymentResponse mapToResponse(Payment payment, Member member) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .memberId(payment.getMemberId())
                .memberName(member != null ? member.getFullName() : "Unknown")
                .subscriptionId(payment.getSubscriptionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentMethodDisplay(payment.getPaymentMethod()!=null?payment.getPaymentMethod().getDisplayName():null)
                .notes(payment.getNotes())
                .recordedBy(payment.getRecordedBy())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}