package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    @Query("SELECT a FROM AuthAuditLog a WHERE a.userId = :userId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuthAuditLog> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuthAuditLog a WHERE a.ipAddress = :ip AND a.eventType = 'LOGIN_FAILED' AND a.createdAt >= :since")
    Long countFailedLoginsByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuthAuditLog a WHERE a.userId = :userId AND a.eventType = 'LOGIN_FAILED' AND a.createdAt >= :since")
    Long countFailedLoginsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}