package com.nitin.saas.payment.dto;

import com.nitin.saas.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodStats {
    private BigDecimal totalRevenue;
    private Long totalPayments;
    private List<MethodBreakdown> byPaymentMethod;
    private BigDecimal averagePaymentAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MethodBreakdown {
        private PaymentMethod method;
        private String methodDisplay;
        private Long count;
        private BigDecimal totalAmount;
        private Double percentage;
    }
}