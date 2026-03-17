package com.nitin.saas.auth.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank @Email @Size(max=255) private String email;
    @NotBlank @Size(min=8,max=100) private String password;
    @Size(max=100) private String firstName;
    @Size(max=100) private String lastName;
    @Size(max=20)  private String phoneNumber;
    @Size(max=10)  private String preferredLanguage;
    @Size(max=50)  private String timezone;
}