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
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDetailResponse {
    // Basic Info
    private Long id;
    private String publicId;
    private Long businessId;
    private String businessPublicId;
    private String businessName;
    private Long userId;
    private String userPublicId;
    private String userEmail;
    private String userName;

    // Role & Status
    private StaffRole role;
    private String roleDisplay;
    private Staff.StaffStatus status;
    private String statusDisplay;

    // Employment Details
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String department;
    private String designation;
    private BigDecimal salary;
    private String employeeId;

    // Contact Info
    private String phone;
    private String email;
    private String address;
    private String emergencyContact;

    // Permissions
    private Set<StaffRole.Permission> rolePermissions;
    private Set<StaffRole.Permission> grantedPermissions;
    private Set<StaffRole.Permission> revokedPermissions;
    private Set<StaffRole.Permission> effectivePermissions;

    // Capabilities
    private Boolean canLogin;
    private Boolean canManageMembers;
    private Boolean canManagePayments;
    private Boolean canManageSubscriptions;
    private Boolean canViewReports;

    // Additional Info
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Statistics
    private Long totalMembersAdded;
    private Long totalPaymentsRecorded;
    private Long totalSubscriptionsAssigned;
}