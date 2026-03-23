package com.nitin.saas.task.dto;

import com.nitin.saas.task.entity.StaffTask;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateStaffTaskRequest {

    @NotBlank
    @Size(max = 180)
    private String title;

    @Size(max = 4000)
    private String description;

    private StaffTask.Priority priority;

    private LocalDate dueDate;

    /**
     * Accepts staff public ID (STF-...) or legacy numeric ID.
     */
    private String assignedStaffId;
}
