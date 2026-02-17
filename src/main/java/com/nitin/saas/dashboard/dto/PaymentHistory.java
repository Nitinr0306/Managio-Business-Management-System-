package com.nitin.saas.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {
    private Long paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDate paidAt;
    private String notes;
}
