package com.nitin.saas.auth.dto;

public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    public AuthTokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }
}
