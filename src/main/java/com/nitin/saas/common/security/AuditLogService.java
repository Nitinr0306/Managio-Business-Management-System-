package com.nitin.saas.common.security;

public interface AuditLogService {
    void logAuthEvent(String email, String eventType, String status, String ipAddress, String userAgent);
}