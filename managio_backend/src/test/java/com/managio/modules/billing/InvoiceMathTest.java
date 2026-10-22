package com.managio.modules.billing;

import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.domain.InvoiceMath;
import com.managio.modules.billing.domain.InvoiceState;
import com.managio.modules.billing.domain.InvoiceMath.LineInput;
import com.managio.modules.billing.domain.InvoiceMath.Totals;
import com.managio.platform.util.Money;
import com.managio.platform.error.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BIL-1 and MEM-4 arithmetic. */
class InvoiceMathTest {

    private static LineInput line(String quantity, String price, String taxRate) {
        return new LineInput(new BigDecimal(quantity), new BigDecimal(price), new BigDecimal(taxRate));
    }

    @Test
    void addsTaxOnTopOfThePrice() {
        Totals totals = InvoiceMath.compute(List.of(line("2", "500.00", "18")), null);

        assertThat(totals.subtotal()).isEqualByComparingTo("1000.00");
        assertThat(totals.tax()).isEqualByComparingTo("180.00");
        assertThat(totals.total()).isEqualByComparingTo("1180.00");
    }

    @Test
    void aDiscountReducesTheTaxableValueInProportion() {
        Totals totals = InvoiceMath.compute(List.of(line("1", "1000", "18")), new BigDecimal("100"));

        assertThat(totals.tax()).isEqualByComparingTo("162.00");
        assertThat(totals.total()).isEqualByComparingTo("1062.00");
    }

    @Test
    void taxIsRoundedOnceForTheWholeInvoice() {
        // Three lines of 0.33 at 5% would round to 0.02 each (0.06) line by line, but 0.0495 -> 0.05 once.
        Totals totals = InvoiceMath.compute(List.of(line("1", "0.33", "5"), line("1", "0.33", "5"), line("1", "0.33", "5")), null);

        assertThat(totals.tax()).isEqualByComparingTo("0.05");
        assertThat(totals.total()).isEqualByComparingTo("1.04");
    }

    @Test
    void mixedTaxRatesAreAppliedPerLine() {
        Totals totals = InvoiceMath.compute(List.of(line("1", "100", "18"), line("1", "100", "0")), null);

        assertThat(totals.tax()).isEqualByComparingTo("18.00");
        assertThat(totals.lineAmounts()).hasSize(2);
    }

    @Test
    void aDiscountLargerThanTheInvoiceIsRejected() {
        assertThatThrownBy(() -> InvoiceMath.compute(List.of(line("1", "100", "0")), new BigDecimal("100.01")))
                .isInstanceOf(ApiException.class);
        assertThat(InvoiceMath.compute(List.of(line("1", "100", "0")), new BigDecimal("100")).total()).isEqualByComparingTo("0");
    }

    @Test
    void installmentsSplitEvenlyAndTheLastAbsorbsRounding() {
        List<BigDecimal> shares = InvoiceMath.splitEvenly(new BigDecimal("100.00"), 3);

        assertThat(shares).containsExactly(Money.of("33.33"), Money.of("33.33"), Money.of("33.34"));
        assertThat(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
    }

    @Test
    void installmentsAreDueAtEvenlySpacedDatesFromTheStart() {
        // 90 days from 20 Oct 2026 ends 17 Jan 2027.
        List<LocalDate> dates = InvoiceMath.installmentDueDates(LocalDate.of(2026, 10, 20), LocalDate.of(2027, 1, 17), 3);

        assertThat(dates).containsExactly(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 19), LocalDate.of(2026, 12, 19));
        assertThat(InvoiceMath.installmentDueDates(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 18), 1))
                .containsExactly(LocalDate.of(2026, 10, 20));
    }

    @Test
    void invoiceStateIsOverdueBeforePartlyPaid() {
        LocalDate today = LocalDate.of(2026, 10, 20);
        BigDecimal total = new BigDecimal("100.00");

        assertThat(InvoiceState.of(Invoice.Status.ISSUED, total, BigDecimal.ZERO, today, today)).isEqualTo(InvoiceState.DUE);
        assertThat(InvoiceState.of(Invoice.Status.ISSUED, total, new BigDecimal("40"), today, today)).isEqualTo(InvoiceState.PARTLY_PAID);
        assertThat(InvoiceState.of(Invoice.Status.ISSUED, total, new BigDecimal("40"), today.minusDays(1), today)).isEqualTo(InvoiceState.OVERDUE);
        assertThat(InvoiceState.of(Invoice.Status.ISSUED, total, total, today.minusDays(9), today)).isEqualTo(InvoiceState.PAID);
        assertThat(InvoiceState.of(Invoice.Status.VOID, total, total, today, today)).isEqualTo(InvoiceState.VOID);
    }

    @Test
    void rupeesUseIndianGrouping() {
        assertThat(Money.inr(new BigDecimal("124500"))).isEqualTo("₹1,24,500");
        assertThat(Money.inr(new BigDecimal("6499.50"))).isEqualTo("₹6,499.50");
        assertThat(Money.inr(new BigDecimal("41500.00"))).isEqualTo("₹41,500");
        assertThat(Money.inr(new BigDecimal("12345678"))).isEqualTo("₹1,23,45,678");
        assertThat(Money.inr(new BigDecimal("999"))).isEqualTo("₹999");
    }
}
