package com.managio.modules.billing.domain;

/** How money moved. Shared by payments, refunds and expenses. */
public enum PaymentMethod {
    CASH, UPI, CARD, BANK_TRANSFER, CHEQUE, ONLINE, OTHER
}
