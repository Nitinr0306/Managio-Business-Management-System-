package com.nitin.saas.payment.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.payment.dto.PaymentMethodStats;
import com.nitin.saas.payment.enums.PaymentMethod;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.staff.enums.StaffRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentStatsService {

    private final PaymentRepository paymentRepository;
    private final BusinessService   businessService;

    /**
     * Returns per-method aggregation using a single SQL GROUP BY query.
     * Never loads full payment rows into JVM memory.
     */
    @Transactional(readOnly = true)
    public PaymentMethodStats getPaymentMethodStats(Long businessId) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_PAYMENTS);

        List<Object[]> rows = paymentRepository.getPaymentStatsGroupedByMethod(businessId);

        BigDecimal totalRevenue  = BigDecimal.ZERO;
        long       totalPayments = 0L;
        List<PaymentMethodStats.MethodBreakdown> breakdowns = new ArrayList<>();

        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            Long          count  = (Long)          row[1];
            BigDecimal    amount = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            totalRevenue  = totalRevenue.add(amount);
            totalPayments += count;
            breakdowns.add(PaymentMethodStats.MethodBreakdown.builder()
                    .method(method).methodDisplay(method.getDisplayName())
                    .count(count).totalAmount(amount).percentage(0.0)
                    .build());
        }

        final BigDecimal total = totalRevenue;
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            breakdowns.forEach(b -> b.setPercentage(
                    b.getTotalAmount().divide(total, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()));
        }

        breakdowns.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        BigDecimal avg = totalPayments > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PaymentMethodStats.builder()
                .totalRevenue(totalRevenue)
                .totalPayments(totalPayments)
                .byPaymentMethod(breakdowns)
                .averagePaymentAmount(avg)
                .build();
    }
}