package com.nitin.saas.audit.repository;

import com.nitin.saas.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByBusinessIdOrderByCreatedAtDesc(Long businessId, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.businessId = :businessId AND a.entityType = :entityType ORDER BY a.createdAt DESC")
    Page<AuditLog> findByBusinessIdAndEntityType(
            @Param("businessId") Long businessId,
            @Param("entityType") String entityType,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.businessId = :businessId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(
            @Param("businessId") Long businessId,
            @Param("since") LocalDateTime since);
}