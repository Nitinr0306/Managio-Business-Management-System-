package com.nitin.saas.task.repository;

import com.nitin.saas.task.entity.StaffTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffTaskRepository extends JpaRepository<StaffTask, Long> {

    @Query("SELECT t FROM StaffTask t WHERE t.businessId = :businessId " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:assignedStaffId IS NULL OR t.assignedStaffId = :assignedStaffId) " +
            "ORDER BY CASE WHEN t.dueDate IS NULL THEN 1 ELSE 0 END, t.dueDate ASC, t.createdAt DESC")
    Page<StaffTask> findByFilters(@Param("businessId") Long businessId,
                                  @Param("status") StaffTask.Status status,
                                  @Param("assignedStaffId") Long assignedStaffId,
                                  Pageable pageable);

    @Query("SELECT t FROM StaffTask t WHERE UPPER(t.publicId) = UPPER(:publicId) AND t.businessId = :businessId")
    Optional<StaffTask> findByPublicIdAndBusinessId(@Param("publicId") String publicId,
                                                    @Param("businessId") Long businessId);
}
