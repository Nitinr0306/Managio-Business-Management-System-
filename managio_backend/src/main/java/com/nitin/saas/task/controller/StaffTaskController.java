package com.nitin.saas.task.controller;

import com.nitin.saas.task.dto.CreateStaffTaskRequest;
import com.nitin.saas.task.dto.StaffTaskResponse;
import com.nitin.saas.task.dto.UpdateStaffTaskRequest;
import com.nitin.saas.task.dto.UpdateTaskStatusRequest;
import com.nitin.saas.task.entity.StaffTask;
import com.nitin.saas.task.service.StaffTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Staff Tasks", description = "Task management for business staff")
public class StaffTaskController {

    private final StaffTaskService staffTaskService;

    @GetMapping
    @Operation(summary = "Get tasks for business")
    public ResponseEntity<Page<StaffTaskResponse>> getTasks(
            @PathVariable Long businessId,
            @RequestParam(required = false) StaffTask.Status status,
            @RequestParam(required = false) String assignedStaffId,
            @RequestParam(defaultValue = "false") Boolean assignedToMe,
            Pageable pageable) {
        return ResponseEntity.ok(staffTaskService.getTasks(businessId, status, assignedStaffId, assignedToMe, pageable));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task details")
    public ResponseEntity<StaffTaskResponse> getTask(
            @PathVariable Long businessId,
            @PathVariable String taskId) {
        return ResponseEntity.ok(staffTaskService.getTask(businessId, taskId));
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<StaffTaskResponse> createTask(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateStaffTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(staffTaskService.createTask(businessId, request));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task")
    public ResponseEntity<StaffTaskResponse> updateTask(
            @PathVariable Long businessId,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateStaffTaskRequest request) {
        return ResponseEntity.ok(staffTaskService.updateTask(businessId, taskId, request));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update task status")
    public ResponseEntity<StaffTaskResponse> updateTaskStatus(
            @PathVariable Long businessId,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(staffTaskService.updateStatus(businessId, taskId, request.getStatus()));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long businessId,
            @PathVariable String taskId) {
        staffTaskService.deleteTask(businessId, taskId);
        return ResponseEntity.noContent().build();
    }
}
