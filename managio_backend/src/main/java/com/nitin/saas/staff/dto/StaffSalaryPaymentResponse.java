package com.nitin.saas.staff.dto;

import com.nitin.saas.staff.entity.StaffSalaryPayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalaryPaymentResponse {
    private Long id;
    private Long staffId;
    private String staffPublicId;
    private String staffName;
    private String employeeId;
    private LocalDate salaryMonth;
    private BigDecimal monthlySalary;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private StaffSalaryPayment.PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private Boolean manuallyMarked;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
