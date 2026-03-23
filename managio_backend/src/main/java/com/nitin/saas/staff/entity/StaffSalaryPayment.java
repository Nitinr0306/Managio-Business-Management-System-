package com.nitin.saas.staff.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_salary_payments", indexes = {
        @Index(name = "idx_staff_salary_staff", columnList = "staffId"),
        @Index(name = "idx_staff_salary_month", columnList = "salaryMonth"),
        @Index(name = "idx_staff_salary_status", columnList = "paymentStatus")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_salary_staff_month", columnNames = {"staffId", "salaryMonth"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalaryPayment {

    public enum PaymentStatus {
        PAID,
        UNPAID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long staffId;

    @Column(nullable = false)
    private LocalDate salaryMonth;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlySalary;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean manuallyMarked = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
