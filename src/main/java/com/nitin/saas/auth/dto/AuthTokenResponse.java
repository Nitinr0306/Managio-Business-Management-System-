package com.nitin.saas.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTokenResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private String scope;
}