package com.nitin.saas.auth.dto;

import com.nitin.saas.auth.enums.Role;
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
public class UserResponse {

    private Long id;

    private String email;

    private String firstName;

    private String lastName;

    private String fullName;

    private String phoneNumber;

    private Set<Role> roles;

    private Boolean emailVerified;

    private Boolean enabled;

    private Boolean accountLocked;

    private String accountStatus;

    private String profileImageUrl;

    private String preferredLanguage;

    private String timezone;

    private Boolean twoFactorEnabled;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}