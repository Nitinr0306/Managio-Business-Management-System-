package com.nitin.saas.task.dto;

import com.nitin.saas.task.entity.StaffTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class StaffTaskResponse {
    private Long id;
    private String publicId;
    private Long businessId;
    private String title;
    private String description;
    private StaffTask.Status status;
    private StaffTask.Priority priority;
    private LocalDate dueDate;
    private Long assignedStaffId;
    private String assignedStaffPublicId;
    private String assignedStaffName;
    private Long createdByUserId;
    private String createdByUserPublicId;
    private String createdByUserName;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
