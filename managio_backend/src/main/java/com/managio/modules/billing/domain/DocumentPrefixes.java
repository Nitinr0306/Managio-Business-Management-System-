package com.managio.modules.billing.domain;

/** A business's invoice and receipt number prefixes, which turn stored sequence numbers into display numbers. */
public record DocumentPrefixes(String invoice, String receipt) {

    public String invoiceNumber(int number) {
        return "%s-%04d".formatted(invoice, number);
    }

    public String receiptNumber(int number) {
        return "%s-%04d".formatted(receipt, number);
    }
}
