package com.nitin.saas.staff.dto;

import com.nitin.saas.staff.enums.StaffRole;
import jakarta.validation.constraints.*;
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
public class CreateStaffRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role is required")
    private StaffRole role;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    private LocalDate hireDate;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @PositiveOrZero(message = "Salary must be positive or zero")
    private BigDecimal salary;

    @Size(max = 20, message = "Employee ID must not exceed 20 characters")
    private String employeeId;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    @Size(max = 2000, message = "Emergency contact must not exceed 2000 characters")
    private String emergencyContact;

    @Builder.Default
    private Boolean canLogin = true;

    @Builder.Default
    private Boolean canManageMembers = false;

    @Builder.Default
    private Boolean canManagePayments = false;

    @Builder.Default
    private Boolean canManageSubscriptions = false;

    @Builder.Default
    private Boolean canViewReports = false;
}