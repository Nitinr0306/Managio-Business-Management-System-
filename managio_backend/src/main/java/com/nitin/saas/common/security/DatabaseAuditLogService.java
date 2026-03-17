package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseAuditLogService implements AuditLogService {

    private final AuthAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logAuthEvent(String email, String eventType, String status, String ipAddress, String userAgent) {
        try {
            AuthAuditLog auditLog = AuthAuditLog.builder()
                    .email(email)
                    .eventType(AuthAuditLog.EventType.valueOf(eventType))
                    .status(AuthAuditLog.Status.valueOf(status))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Auth event logged: {} - {}", eventType, status);
        } catch (Exception e) {
            log.error("Failed to log auth event", e);
        }
    }
}