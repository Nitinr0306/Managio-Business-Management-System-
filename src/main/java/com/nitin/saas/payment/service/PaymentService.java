package com.nitin.saas.payment.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.service.BusinessAuthorizationService;
import com.nitin.saas.common.audit.service.AuditService;
import com.nitin.saas.payment.enums.PaymentStatus;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.enums.PaymentProvider;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.service.SubscriptionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BusinessAuthorizationService authorizationService;
    private final SubscriptionService subscriptionService;
    private final AuditService auditService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BusinessAuthorizationService authorizationService,
            SubscriptionService subscriptionService,
            AuditService auditService
    ) {
        this.paymentRepository = paymentRepository;
        this.authorizationService = authorizationService;
        this.subscriptionService = subscriptionService;
        this.auditService = auditService;
    }

    @Transactional
    public Payment createPayment(Business business, MemberSubscription subscription, int amount, PaymentProvider provider, String idempotencyKey){
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(()->{
                    Payment payment = new Payment(business, subscription, amount, provider, idempotencyKey);
                    return paymentRepository.save(payment);
                });
    }
    @Transactional
    public void markPaymentSuccess(Payment payment, String externalPaymentId){
        payment.markSuccess(externalPaymentId);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markPaymentFailed(Payment payment){
        if(payment.getStatus()== PaymentStatus.SUCCESS){
            return;
        }
        payment.markFailed();
        paymentRepository.save(payment);
    }

    @Transactional
    public Payment retryPayment(
            Business business,
            MemberSubscription subscription,
            int amount,
            PaymentProvider provider,
            String newIdempotencyKey
    ){
        return createPayment(business, subscription, amount, provider, newIdempotencyKey);
    }

    @Transactional
    public Payment recordManualPayment(
            Business business,
            User requester,
            MemberSubscription subscription,
            int amount,
            String referenceNote
    ){
        authorizationService.authorizeOwnerOrStaff(business, requester);
        Payment payment=new Payment(
                business,
                subscription,
                amount,
                PaymentProvider.MANUAL,
                "MANUAL-"+System.currentTimeMillis()
        );
        payment.markSuccess(referenceNote);
        paymentRepository.save(payment);

        subscriptionService.activateSubscriptionAfterPayment(subscription);
        auditService.log(
                "MANUAL_PAYMENT",
                "PAYMENT",
                payment.getId(),
                requester.getId(),
                referenceNote
        );


        return payment;
    }
    
}
