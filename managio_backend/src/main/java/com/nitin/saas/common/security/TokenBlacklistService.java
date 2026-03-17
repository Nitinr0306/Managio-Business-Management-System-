package com.nitin.saas.common.security;

public interface TokenBlacklistService {
    void blacklist(String token, long expirySeconds);
    boolean isBlacklisted(String token);
    void cleanup();
}