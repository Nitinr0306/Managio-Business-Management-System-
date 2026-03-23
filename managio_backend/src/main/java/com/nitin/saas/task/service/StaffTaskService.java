package com.nitin.saas.task.service;

import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.repository.StaffRepository;
import com.nitin.saas.task.dto.CreateStaffTaskRequest;
import com.nitin.saas.task.dto.StaffTaskResponse;
import com.nitin.saas.task.dto.UpdateStaffTaskRequest;
import com.nitin.saas.task.entity.StaffTask;
import com.nitin.saas.task.repository.StaffTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StaffTaskService {

    private final StaffTaskRepository taskRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final BusinessService businessService;
    private final RBACService rbacService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<StaffTaskResponse> getTasks(Long businessId,
                                            StaffTask.Status status,
                                            String assignedStaffIdentifier,
                                            Boolean assignedToMe,
                                            Pageable pageable) {
        businessService.requireAccess(businessId);

        Long assignedStaffId = null;
        if (Boolean.TRUE.equals(assignedToMe)) {
            assignedStaffId = resolveCurrentStaffId(businessId);
        } else if (assignedStaffIdentifier != null && !assignedStaffIdentifier.isBlank()) {
            assignedStaffId = resolveStaff(businessId, assignedStaffIdentifier).getId();
        }

        return taskRepository.findByFilters(businessId, status, assignedStaffId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StaffTaskResponse getTask(Long businessId, String taskIdentifier) {
        businessService.requireAccess(businessId);
        return toResponse(resolveTask(businessId, taskIdentifier));
    }

    @Transactional
    public StaffTaskResponse createTask(Long businessId, CreateStaffTaskRequest request) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.EDIT_STAFF);

        Long currentUserId = rbacService.getCurrentUserId();

        Staff assignee = null;
        if (request.getAssignedStaffId() != null && !request.getAssignedStaffId().isBlank()) {
            assignee = resolveStaff(businessId, request.getAssignedStaffId());
        }

        StaffTask task = StaffTask.builder()
                .businessId(businessId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : StaffTask.Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .assignedStaffId(assignee != null ? assignee.getId() : null)
                .createdByUserId(currentUserId)
                .build();

        task = taskRepository.save(task);

        auditLogService.logAction(
                businessId,
                "TASK_CREATED",
                "TASK",
                task.getId(),
                "task=" + task.getTitle() + ", assignee=" + (assignee != null ? assignee.getPublicId() : "UNASSIGNED")
        );

        return toResponse(task);
    }

    @Transactional
    public StaffTaskResponse updateTask(Long businessId, String taskIdentifier, UpdateStaffTaskRequest request) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.EDIT_STAFF);

        StaffTask task = resolveTask(businessId, taskIdentifier);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssignedStaffId() != null) {
            if (request.getAssignedStaffId().isBlank()) {
                task.setAssignedStaffId(null);
            } else {
                task.setAssignedStaffId(resolveStaff(businessId, request.getAssignedStaffId()).getId());
            }
        }

        task = taskRepository.save(task);

        auditLogService.logAction(
                businessId,
                "TASK_UPDATED",
                "TASK",
                task.getId(),
                "task=" + task.getTitle() + ", status=" + task.getStatus()
        );

        return toResponse(task);
    }

    @Transactional
    public StaffTaskResponse updateStatus(Long businessId, String taskIdentifier, StaffTask.Status status) {
        businessService.requireAccess(businessId);

        StaffTask task = resolveTask(businessId, taskIdentifier);

        if (status == StaffTask.Status.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (task.getStatus() == StaffTask.Status.COMPLETED) {
            task.setCompletedAt(null);
        }
        task.setStatus(status);

        task = taskRepository.save(task);

        auditLogService.logAction(
                businessId,
                "TASK_STATUS_CHANGED",
                "TASK",
                task.getId(),
                "task=" + task.getTitle() + ", status=" + status
        );

        return toResponse(task);
    }

    @Transactional
    public void deleteTask(Long businessId, String taskIdentifier) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.EDIT_STAFF);

        StaffTask task = resolveTask(businessId, taskIdentifier);
        taskRepository.delete(task);

        auditLogService.logAction(
                businessId,
                "TASK_DELETED",
                "TASK",
                task.getId(),
                "task=" + task.getTitle()
        );
    }

    private StaffTask resolveTask(Long businessId, String taskIdentifier) {
        String value = normalizeIdentifier(taskIdentifier, "Task identifier is required");

        if (value.startsWith("TSK")) {
            return taskRepository.findByPublicIdAndBusinessId(value, businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskIdentifier));
        }

        try {
            Long id = Long.valueOf(value);
            return taskRepository.findById(id)
                    .filter(t -> t.getBusinessId().equals(businessId))
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskIdentifier));
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid task identifier format");
        }
    }

    private Staff resolveStaff(Long businessId, String staffIdentifier) {
        String value = normalizeIdentifier(staffIdentifier, "Staff identifier is required");

        Staff staff;
        if (value.startsWith("STF-")) {
            staff = staffRepository.findActiveByPublicId(value)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffIdentifier));
        } else {
            try {
                Long id = Long.valueOf(value);
                staff = staffRepository.findActiveById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffIdentifier));
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid staff identifier format");
            }
        }

        if (!staff.getBusinessId().equals(businessId)) {
            throw new BadRequestException("Staff does not belong to this business");
        }

        return staff;
    }

    private Long resolveCurrentStaffId(Long businessId) {
        Long currentUserId = rbacService.getCurrentUserId();
        return staffRepository.findByBusinessIdAndUserId(businessId, currentUserId)
                .map(Staff::getId)
                .orElseThrow(() -> new ResourceNotFoundException("No staff profile for current user in business"));
    }

    private String normalizeIdentifier(String value, String emptyError) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BadRequestException(emptyError);
        }
        return normalized;
    }

    private StaffTaskResponse toResponse(StaffTask task) {
        Staff assignee = task.getAssignedStaffId() != null
                ? staffRepository.findById(task.getAssignedStaffId()).orElse(null)
                : null;

        User assigneeUser = assignee != null
                ? userRepository.findById(assignee.getUserId()).orElse(null)
                : null;

        User creator = userRepository.findById(task.getCreatedByUserId()).orElse(null);

        String assigneeName = null;
        if (assigneeUser != null && assigneeUser.getFullName() != null && !assigneeUser.getFullName().isBlank()) {
            assigneeName = assigneeUser.getFullName();
        } else if (assigneeUser != null) {
            assigneeName = assigneeUser.getEmail();
        }

        String creatorName = null;
        if (creator != null && creator.getFullName() != null && !creator.getFullName().isBlank()) {
            creatorName = creator.getFullName();
        } else if (creator != null) {
            creatorName = creator.getEmail();
        }

        return StaffTaskResponse.builder()
                .id(task.getId())
                .publicId(task.getPublicId())
                .businessId(task.getBusinessId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedStaffId(task.getAssignedStaffId())
                .assignedStaffPublicId(assignee != null ? assignee.getPublicId() : null)
                .assignedStaffName(assigneeName)
                .createdByUserId(task.getCreatedByUserId())
                .createdByUserPublicId(creator != null ? creator.getPublicId() : null)
                .createdByUserName(creatorName)
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
