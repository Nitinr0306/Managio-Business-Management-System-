package com.nitin.saas.staff.repository;

import com.nitin.saas.staff.entity.StaffPermission;
import com.nitin.saas.staff.enums.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffPermissionRepository extends JpaRepository<StaffPermission, Long> {

    List<StaffPermission> findByStaffId(Long staffId);

    @Query("SELECT sp FROM StaffPermission sp WHERE sp.staffId = :staffId AND sp.permission = :permission")
    Optional<StaffPermission> findByStaffIdAndPermission(
            @Param("staffId") Long staffId,
            @Param("permission") StaffRole.Permission permission);

    @Query("SELECT sp FROM StaffPermission sp WHERE sp.staffId = :staffId AND sp.granted = true")
    List<StaffPermission> findGrantedPermissions(@Param("staffId") Long staffId);

    @Query("SELECT sp FROM StaffPermission sp WHERE sp.staffId = :staffId AND sp.granted = false")
    List<StaffPermission> findRevokedPermissions(@Param("staffId") Long staffId);

    @Modifying
    @Query("DELETE FROM StaffPermission sp WHERE sp.staffId = :staffId")
    void deleteByStaffId(@Param("staffId") Long staffId);

    boolean existsByStaffIdAndPermission(Long staffId, StaffRole.Permission permission);
}