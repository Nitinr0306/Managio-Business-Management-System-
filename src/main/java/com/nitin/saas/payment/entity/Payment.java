package com.nitin.saas.payment.entity;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.payment.enums.PaymentProvider;
import com.nitin.saas.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name="payments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "idempotencyKey")
        })
public class Payment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        private Business business;

        @ManyToOne(optional = false)
        private MemberSubscription subscription;

        @Column(nullable = false)
        private int amount;

        @Enumerated(EnumType.STRING)
        private PaymentStatus status;

        @Enumerated(EnumType.STRING)
        private PaymentProvider provider;

        @Column(nullable = false, unique = true)
        private String idempotencyKey;

        private String externalPaymentId;

        private LocalDateTime createdAt;

        protected Payment() {}

        public Payment(
                Business business,
                MemberSubscription subscription,
                int amount,
                PaymentProvider provider,
                String idempotencyKey
        ) {
                this.business = business;
                this.subscription = subscription;
                this.amount = amount;
                this.provider = provider;
                this.idempotencyKey = idempotencyKey;
                this.status = PaymentStatus.CREATED;
                this.createdAt = LocalDateTime.now();
        }

        public void markSuccess(String externalPaymentId) {
                this.status = PaymentStatus.SUCCESS;
                this.externalPaymentId = externalPaymentId;
        }

        public void markFailed() {
                this.status = PaymentStatus.FAILED;
        }
}
