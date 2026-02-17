package com.nitin.saas.payment.enums;


public enum PaymentMethod {
    CASH("Cash"),
    UPI("UPI"),
    NETBANKING("Net Banking"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    PAYTM("Paytm"),
    PHONEPE("PhonePe"),
    GOOGLEPAY("Google Pay"),
    RAZORPAY("Razorpay"),
    STRIPE("Stripe"),
    CHEQUE("Cheque"),
    BANK_TRANSFER("Bank Transfer");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}