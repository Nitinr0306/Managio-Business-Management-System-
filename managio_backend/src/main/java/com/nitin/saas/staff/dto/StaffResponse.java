package com.nitin.saas.staff.dto;

import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
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
public class StaffResponse {
    private Long id;
    private String publicId;
    private Long businessId;
    private String businessPublicId;
    private Long userId;
    private String userPublicId;
    private String userEmail;
    private String userName;
    private StaffRole role;
    private String roleDisplay;
    private Staff.StaffStatus status;
    private String statusDisplay;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String department;
    private String designation;
    private BigDecimal salary;
    private String employeeId;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private Boolean canLogin;
    private Boolean canManageMembers;
    private Boolean canManagePayments;
    private Boolean canManageSubscriptions;
    private Boolean canViewReports;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}