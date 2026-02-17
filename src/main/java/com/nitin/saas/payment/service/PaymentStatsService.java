package com.nitin.saas.payment.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.payment.dto.PaymentMethodStats;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.enums.PaymentMethod;
import com.nitin.saas.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentStatsService {

    private final PaymentRepository paymentRepository;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public PaymentMethodStats getPaymentMethodStats(Long businessId) {
        businessService.requireAccess(businessId);

        LocalDateTime start = LocalDateTime.now().minusYears(10); // All time

        BigDecimal totalRevenue = paymentRepository.calculateRevenue(businessId, start);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        // Get all payments for this business
        List<com.nitin.saas.payment.entity.Payment> payments =
                paymentRepository.findRecentPayments(businessId, start);

        Long totalPayments = (long) payments.size();

        // Group by payment method
        Map<PaymentMethod, List<Payment>> byMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod));

        List<PaymentMethodStats.MethodBreakdown> breakdowns = new ArrayList<>();

        for (Map.Entry<PaymentMethod, List<Payment>> entry : byMethod.entrySet()) {
            PaymentMethod method = entry.getKey();
            List<Payment> methodPayments = entry.getValue();

            Long count = (long) methodPayments.size();
            BigDecimal amount = methodPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).doubleValue()
                    : 0.0;

            breakdowns.add(PaymentMethodStats.MethodBreakdown.builder()
                    .method(method)
                    .methodDisplay(method.getDisplayName())
                    .count(count)
                    .totalAmount(amount)
                    .percentage(percentage)
                    .build());
        }

        // Sort by amount descending
        breakdowns.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        BigDecimal averagePayment = totalPayments > 0
                ? totalRevenue.divide(new BigDecimal(totalPayments), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PaymentMethodStats.builder()
                .totalRevenue(totalRevenue)
                .totalPayments(totalPayments)
                .byPaymentMethod(breakdowns)
                .averagePaymentAmount(averagePayment)
                .build();
    }
}