package com.nitin.saas.payment.service;

import com.nitin.saas.subscription.service.SubscriptionService;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.enums.PaymentStatus;
import com.nitin.saas.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PaymentWebhookService {
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    public PaymentWebhookService(
            PaymentRepository paymentRepository,
            PaymentService paymentService,
            SubscriptionService subscriptionService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public void handlePaymentSuccess(
            String idempotencyKey,
            String externalPaymentId
    ) {
        Payment payment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() ->
                        new IllegalStateException("Payment not found")
                );

        if (alreadyProcessed(payment)) return;

        paymentService.markPaymentSuccess(payment, externalPaymentId);
        subscriptionService.activateSubscriptionAfterPayment(
                payment.getSubscription()
        );
    }

    private boolean alreadyProcessed(Payment payment) {
        return payment.getStatus() == PaymentStatus.SUCCESS;
    }
    @Transactional
    public void handlePaymentFailure(String idempotencyKey){
        Payment payment=paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(()->new IllegalStateException("Payment not found"));
        paymentService.markPaymentFailed(payment);
    }
}
