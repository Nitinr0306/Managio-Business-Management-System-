package com.nitin.saas.task.dto;

import com.nitin.saas.task.entity.StaffTask;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {

    @NotNull
    private StaffTask.Status status;
}
