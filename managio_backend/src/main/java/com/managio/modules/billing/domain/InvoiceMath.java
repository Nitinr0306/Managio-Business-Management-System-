package com.managio.modules.billing.domain;

import com.managio.platform.util.Money;
import com.managio.platform.error.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice arithmetic (BIL-1, MEM-4). Prices are before tax. The discount applies to the whole invoice and
 * reduces every line's taxable value in proportion. Tax is summed unrounded and rounded once, half-up.
 */
public final class InvoiceMath {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private InvoiceMath() {
    }

    public record LineInput(BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
    }

    public record Totals(List<BigDecimal> lineAmounts, BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                         BigDecimal total) {
    }

    public static Totals compute(List<LineInput> lines, BigDecimal discountInput) {
        BigDecimal discount = Money.of(discountInput);
        if (discount.signum() < 0) {
            throw ApiException.invalidField("discount", "A discount can't be negative");
        }
        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal subtotal = Money.ZERO;
        for (LineInput line : lines) {
            BigDecimal amount = Money.of(line.quantity().multiply(line.unitPrice()));
            amounts.add(amount);
            subtotal = subtotal.add(amount);
        }
        if (discount.compareTo(subtotal) > 0) {
            throw ApiException.invalidField("discount", "The discount can't be more than the invoice amount");
        }
        BigDecimal tax = Money.ZERO;
        if (subtotal.signum() > 0) {
            BigDecimal rawTax = BigDecimal.ZERO;
            for (int i = 0; i < lines.size(); i++) {
                rawTax = rawTax.add(amounts.get(i).multiply(lines.get(i).taxRate()).divide(HUNDRED, 10, RoundingMode.HALF_UP));
            }
            BigDecimal taxableShare = subtotal.subtract(discount).divide(subtotal, 10, RoundingMode.HALF_UP);
            tax = Money.of(rawTax.multiply(taxableShare));
        }
        return new Totals(amounts, subtotal, discount, tax, subtotal.subtract(discount).add(tax));
    }

    /** Splits {@code total} into {@code parts} equal amounts; the last absorbs the rounding difference. */
    public static List<BigDecimal> splitEvenly(BigDecimal total, int parts) {
        BigDecimal each = total.divide(BigDecimal.valueOf(parts), 2, RoundingMode.HALF_UP);
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal allocated = Money.ZERO;
        for (int i = 0; i < parts - 1; i++) {
            shares.add(each);
            allocated = allocated.add(each);
        }
        shares.add(total.subtract(allocated));
        return shares;
    }

    /** Due dates for installments, evenly spaced from the start; the first is due on the start date. */
    public static List<LocalDate> installmentDueDates(LocalDate start, LocalDate end, int parts) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            dates.add(start.plusDays(i * days / parts));
        }
        return dates;
    }
}
