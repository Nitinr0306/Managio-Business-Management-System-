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
    private Long subscriptionId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String paymentMethodDisplay;
    private String notes;
    private Long recordedBy;
    private LocalDateTime createdAt;
}