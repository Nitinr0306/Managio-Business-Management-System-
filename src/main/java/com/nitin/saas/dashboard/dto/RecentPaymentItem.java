package com.nitin.saas.dashboard.dto;

import com.nitin.saas.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecentPaymentItem {

    public Long paymentId;
    public String memberName;
    public int amount;
    public PaymentStatus status;
    public LocalDateTime createdAt;

    public RecentPaymentItem(
            Long paymentId,
            String memberName,
            int amount,
            PaymentStatus status,
            LocalDateTime createdAt
    ) {
        this.paymentId = paymentId;
        this.memberName = memberName;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }
}
