package com.nitin.saas.common.audit.service;



import com.nitin.saas.common.audit.entity.AuditLog;
import com.nitin.saas.common.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(
            String action,
            String entityType,
            Long entityId,
            Long actorUserId,
            String reason
    ) {
        AuditLog log = new AuditLog(
                action,
                entityType,
                entityId,
                actorUserId,
                reason
        );
        repository.save(log);
    }
}

