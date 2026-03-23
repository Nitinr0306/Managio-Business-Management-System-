package com.nitin.saas.staff.dto;

import com.nitin.saas.staff.enums.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitationResponse {
    private Long id;
    private Long businessId;
    private String businessPublicId;
    private String businessName;
    private String email;
    private StaffRole role;
    private String roleDisplay;
    private String token;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime usedAt;
    private Long acceptedByUserId;
    private String acceptedByUserEmail;
    private Long invitedBy;
    private String invitedByUserEmail;
    private String message;
    private String department;
    private String designation;
    private LocalDateTime createdAt;
    private Boolean expired;
    private Boolean valid;
}