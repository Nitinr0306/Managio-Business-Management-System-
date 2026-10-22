package com.managio.platform.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money arithmetic: two decimal places, half-up rounding (docs/ARCHITECTURE.md#time-and-money). */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Rupees with Indian digit grouping for messages: ₹1,24,500 and ₹6,499.50. Whole amounts show no decimals;
     * amounts with paise always show two.
     */
    public static String inr(BigDecimal amount) {
        BigDecimal rounded = of(amount);
        String sign = rounded.signum() < 0 ? "-" : "";
        String[] parts = rounded.abs().toPlainString().split("\\.");
        String whole = parts[0];
        String grouped = whole.length() <= 3 ? whole : group(whole.substring(0, whole.length() - 3)) + "," + whole.substring(whole.length() - 3);
        boolean hasPaise = !"00".equals(parts[1]);
        return "₹" + sign + grouped + (hasPaise ? "." + parts[1] : "");
    }

    /** Groups the digits left of the last three in twos: 12345 becomes 1,23,45. */
    private static String group(String digits) {
        StringBuilder out = new StringBuilder();
        int first = digits.length() % 2;
        if (first > 0) {
            out.append(digits, 0, first);
        }
        for (int i = first; i < digits.length(); i += 2) {
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(digits, i, i + 2);
        }
        return out.toString();
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }
}
