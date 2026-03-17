package com.nitin.saas.business.dto;

import jakarta.validation.constraints.Email;
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
public class CreateBusinessRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 200)
    private String name;

    // FIX 11: all five fields were missing — form submits them, backend silently ignored them.
    @Size(max = 50)
    private String type;

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;
}