package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.lockedAt < :unlockTime")
    List<User> findLockedAccountsToUnlock(@Param("unlockTime") LocalDateTime unlockTime);

    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoffTime")
    List<User> findUnverifiedAccounts(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :inactiveThreshold")
    List<User> findInactiveUsers(@Param("inactiveThreshold") LocalDateTime inactiveThreshold);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countNewUsers(@Param("startDate") LocalDateTime startDate);
}