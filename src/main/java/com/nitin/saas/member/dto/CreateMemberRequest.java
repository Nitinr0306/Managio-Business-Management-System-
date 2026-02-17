package com.nitin.saas.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMemberRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    private LocalDate dateOfBirth;

    @Size(max = 10)
    private String gender;

    @Size(max = 500)
    private String address;

    @Size(max = 2000)
    private String notes;
}