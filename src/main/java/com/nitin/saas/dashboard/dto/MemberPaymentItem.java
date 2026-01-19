package com.nitin.saas.dashboard.dto;


import com.nitin.saas.payment.enums.PaymentProvider;
import com.nitin.saas.payment.enums.PaymentStatus;

import java.time.LocalDateTime;

public class MemberPaymentItem {

    private final LocalDateTime createdAt;
    public long amount;
    public PaymentStatus status;
    public PaymentProvider provider;



    public MemberPaymentItem(
            int amount,
            PaymentStatus status,
            PaymentProvider provider,
            LocalDateTime createdAt
    ) {
        this.amount = amount;
        this.status = status;
        this.provider = provider;
        this.createdAt = createdAt;
    }
}
