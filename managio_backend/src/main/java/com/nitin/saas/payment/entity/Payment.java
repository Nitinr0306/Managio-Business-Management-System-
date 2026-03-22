package com.nitin.saas.payment.entity;

import com.nitin.saas.payment.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_member", columnList = "memberId"),
        @Index(name = "idx_payment_subscription", columnList = "subscriptionId"),
        @Index(name = "idx_payment_created", columnList = "createdAt"),
        @Index(name = "idx_payment_method", columnList = "paymentMethod")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private Long memberId;

        private Long subscriptionId;

        @Column(nullable = false, precision = 10, scale = 2)
        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private PaymentMethod paymentMethod;

        @Column(length = 100)
        private String referenceNumber;

        @Column(length = 500)
        private String notes;

        @Column(nullable = false)
        private Long recordedBy;

        // Business-supplied "paid at" date (defaults to now if not provided)
        private LocalDateTime paidAt;

        @CreationTimestamp
        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;
}