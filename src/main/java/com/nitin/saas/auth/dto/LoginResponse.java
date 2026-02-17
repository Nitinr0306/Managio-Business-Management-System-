package com.nitin.saas.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private UserResponse user;

    private Boolean requiresTwoFactor;

    private LocalDateTime lastLoginAt;

    private String message;
}