package com.nitin.saas.payment.dto;

import com.nitin.saas.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long memberId;
    private String memberName;
    private String memberPhone;
    private Long subscriptionId;
    private String planName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String paymentMethodDisplay;
    private String referenceNumber;
    private String notes;
    private Long recordedBy;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}