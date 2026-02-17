package com.nitin.saas.staff.repository;

import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    @Query("SELECT s FROM Staff s WHERE s.businessId = :businessId AND s.deletedAt IS NULL")
    Page<Staff> findActiveByBusinessId(@Param("businessId") Long businessId, Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.businessId = :businessId AND s.deletedAt IS NULL")
    List<Staff> findActiveByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT s FROM Staff s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Staff> findActiveById(@Param("id") Long id);

    @Query("SELECT s FROM Staff s WHERE s.businessId = :businessId AND s.userId = :userId AND s.deletedAt IS NULL")
    Optional<Staff> findByBusinessIdAndUserId(@Param("businessId") Long businessId,
                                              @Param("userId") Long userId);

    @Query("SELECT s FROM Staff s WHERE s.userId = :userId AND s.deletedAt IS NULL")
    List<Staff> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Staff s WHERE s.businessId = :businessId AND s.role = :role AND s.deletedAt IS NULL")
    List<Staff> findByBusinessIdAndRole(@Param("businessId") Long businessId,
                                        @Param("role") StaffRole role);

    @Query("SELECT s FROM Staff s WHERE s.businessId = :businessId AND s.status = :status AND s.deletedAt IS NULL")
    Page<Staff> findByBusinessIdAndStatus(@Param("businessId") Long businessId,
                                          @Param("status") Staff.StaffStatus status,
                                          Pageable pageable);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.businessId = :businessId AND s.deletedAt IS NULL")
    Long countActiveByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.businessId = :businessId AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    Long countActiveStaff(@Param("businessId") Long businessId);

    @Query("SELECT s FROM Staff s WHERE s.businessId = :businessId AND " +
            "(LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.employeeId) LIKE LOWER(CONCAT('%', :query, '%'))) AND s.deletedAt IS NULL")
    Page<Staff> searchStaff(@Param("businessId") Long businessId,
                            @Param("query") String query,
                            Pageable pageable);

    boolean existsByBusinessIdAndUserId(Long businessId, Long userId);

    boolean existsByBusinessIdAndEmployeeId(Long businessId, String employeeId);
}