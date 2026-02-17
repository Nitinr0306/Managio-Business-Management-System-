package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    Page<AuthAuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuthAuditLog> findByEmail(String email, Pageable pageable);

    @Query("SELECT aal FROM AuthAuditLog aal WHERE aal.userId = :userId AND aal.createdAt >= :since ORDER BY aal.createdAt DESC")
    List<AuthAuditLog> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT aal FROM AuthAuditLog aal WHERE aal.eventType = :eventType AND aal.createdAt >= :since")
    List<AuthAuditLog> findByEventTypeAndSince(@Param("eventType") AuthAuditLog.EventType eventType,
                                               @Param("since") LocalDateTime since);

    @Query("SELECT aal FROM AuthAuditLog aal WHERE aal.status = :status AND aal.createdAt >= :since")
    List<AuthAuditLog> findByStatusAndSince(@Param("status") AuthAuditLog.Status status,
                                            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(aal) FROM AuthAuditLog aal WHERE aal.ipAddress = :ipAddress AND aal.eventType = 'LOGIN_FAILED' AND aal.createdAt >= :since")
    Long countFailedLoginsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(aal) FROM AuthAuditLog aal WHERE aal.userId = :userId AND aal.eventType = 'LOGIN_FAILED' AND aal.createdAt >= :since")
    Long countFailedLoginsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT aal.ipAddress, COUNT(aal) as count FROM AuthAuditLog aal WHERE aal.eventType = 'LOGIN_FAILED' AND aal.createdAt >= :since GROUP BY aal.ipAddress HAVING COUNT(aal) > :threshold")
    List<Object[]> findSuspiciousIpAddresses(@Param("since") LocalDateTime since, @Param("threshold") Long threshold);
}