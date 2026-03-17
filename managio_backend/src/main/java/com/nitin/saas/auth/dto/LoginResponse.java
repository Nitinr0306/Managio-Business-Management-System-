package com.nitin.saas.auth.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long   expiresIn;
    private UserResponse user;
    private Boolean requiresTwoFactor;
    private LocalDateTime lastLoginAt;
    private String message;
}