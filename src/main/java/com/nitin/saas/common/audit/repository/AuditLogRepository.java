package com.nitin.saas.common.audit.repository;

import com.nitin.saas.common.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}

