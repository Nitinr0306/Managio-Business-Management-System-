package com.nitin.saas.auth.dto;

import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private UserResponse user;

    private StaffInfo staff;

    private BusinessInfo business;

    private Boolean requiresTwoFactor;

    private LocalDateTime lastLoginAt;

    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StaffInfo {
        private Long staffId;
        private String staffPublicId;
        private Long businessId;
        private String businessPublicId;
        private StaffRole role;
        private String roleDisplay;
        private Staff.StaffStatus status;
        private String department;
        private String designation;
        private String employeeId;
        private Set<StaffRole.Permission> permissions;
        private Boolean canLogin;
        private Boolean canManageMembers;
        private Boolean canManagePayments;
        private Boolean canManageSubscriptions;
        private Boolean canViewReports;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BusinessInfo {
        private Long id;
        private String publicId;
        private String name;
        private String address;
        private String phone;
        private String email;
    }
}