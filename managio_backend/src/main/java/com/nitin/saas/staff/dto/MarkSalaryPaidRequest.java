package com.nitin.saas.staff.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class MarkSalaryPaidRequest {
    private LocalDate salaryMonth;

    @DecimalMin(value = "0.0", inclusive = false, message = "Paid amount must be greater than zero")
    private BigDecimal paidAmount;

    private LocalDate paidAt;

    private String notes;
}
