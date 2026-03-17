package com.nitin.saas.staff.dto;

import com.nitin.saas.staff.entity.Staff;
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
public class UpdateStaffRequest {

    private StaffRole role;

    private Staff.StaffStatus status;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String designation;

    @PositiveOrZero
    private BigDecimal salary;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String address;

    @Size(max = 2000)
    private String notes;

    @Size(max = 2000)
    private String emergencyContact;

    private Boolean canLogin;

    private Boolean canManageMembers;

    private Boolean canManagePayments;

    private Boolean canManageSubscriptions;

    private Boolean canViewReports;
}