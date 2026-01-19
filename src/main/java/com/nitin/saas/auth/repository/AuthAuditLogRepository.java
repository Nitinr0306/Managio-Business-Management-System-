package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {}
