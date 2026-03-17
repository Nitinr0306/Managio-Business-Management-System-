package com.nitin.saas.auth.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank @Size(min=1,max=100) private String password;
    private String deviceId;
    private Boolean rememberMe;
    private String twoFactorCode;
}