package com.nitin.saas.staff.repository;

import com.nitin.saas.staff.entity.StaffInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long> {

    Optional<StaffInvitation> findByToken(String token);

    @Query("SELECT si FROM StaffInvitation si WHERE si.businessId = :businessId AND si.email = :email AND si.used = false AND si.expiresAt > :now")
    Optional<StaffInvitation> findPendingInvitation(
            @Param("businessId") Long businessId,
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    @Query("SELECT si FROM StaffInvitation si WHERE si.businessId = :businessId ORDER BY si.createdAt DESC")
    Page<StaffInvitation> findByBusinessId(@Param("businessId") Long businessId, Pageable pageable);

    @Query("SELECT si FROM StaffInvitation si WHERE si.businessId = :businessId AND si.used = false ORDER BY si.createdAt DESC")
    List<StaffInvitation> findPendingInvitations(@Param("businessId") Long businessId);

    @Query("SELECT si FROM StaffInvitation si WHERE si.email = :email AND si.used = false AND si.expiresAt > :now")
    List<StaffInvitation> findValidInvitationsByEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM StaffInvitation si WHERE si.expiresAt < :cutoffDate")
    void deleteExpiredInvitations(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(si) FROM StaffInvitation si WHERE si.businessId = :businessId AND si.email = :email AND si.used = false AND si.expiresAt > :now")
    Long countPendingInvitations(
            @Param("businessId") Long businessId,
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    boolean existsByBusinessIdAndEmailAndUsedFalse(Long businessId, String email);
}