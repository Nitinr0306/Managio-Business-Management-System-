package com.nitin.saas.payment.dto;

import com.nitin.saas.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordPaymentRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    private Long subscriptionId;

    @NotNull(message = "Amount is required")
    @Positive
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String referenceNumber;

    private String notes;

    // ISO date string from frontend, e.g. "2025-03-20"
    private String paidAt;
}