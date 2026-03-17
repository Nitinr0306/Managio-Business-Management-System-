package com.nitin.saas.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLoginRequest {

    @NotBlank(message = "Phone or email is required")
    @Size(max = 255, message = "Identifier must not exceed 255 characters")
    private String identifier; // Can be phone or email

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    private String deviceId;

    private Boolean rememberMe;
}