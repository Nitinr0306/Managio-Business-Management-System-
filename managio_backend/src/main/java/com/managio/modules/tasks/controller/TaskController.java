package com.managio.modules.tasks.controller;

import com.managio.modules.tasks.domain.Task;
import com.managio.modules.tasks.service.TaskService;
import com.managio.modules.tasks.domain.Task.Details;
import com.managio.modules.tasks.service.TaskService.TaskResponse;
import com.managio.modules.tasks.service.TaskService.View;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Tasks are open to every team member; the service decides what each person may see and change (TSK-1, TSK-3). */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/tasks")
@RequiredArgsConstructor
class TaskController {

    private final TaskService tasks;

    record TaskBody(@NotBlank(message = "Enter what needs doing") @Size(max = 180, message = "Use 180 characters or fewer") String title,
                    @Size(max = 4000) String description, Task.Priority priority, LocalDate dueOn, Task.Repeat repeat,
                    Long assigneeId, Long customerId) {
        Details toDetails() {
            return new Details(title, description, priority, dueOn, repeat, assigneeId, customerId);
        }
    }

    record StatusBody(@NotNull(message = "Choose a status") Task.Status status) {
    }

    @GetMapping
    PageResponse<TaskResponse> list(BusinessContext ctx, @RequestParam(defaultValue = "OPEN") View view,
                                    @RequestParam(required = false) Long assigneeId,
                                    @RequestParam(defaultValue = "false") boolean mine,
                                    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return tasks.list(ctx, view, assigneeId, mine, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/{taskId}")
    TaskResponse get(BusinessContext ctx, @PathVariable long taskId) {
        return tasks.get(ctx, taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(BusinessContext ctx, @Valid @RequestBody TaskBody body) {
        return tasks.create(ctx, body.toDetails());
    }

    @PutMapping("/{taskId}")
    TaskResponse update(BusinessContext ctx, @PathVariable long taskId, @Valid @RequestBody TaskBody body) {
        return tasks.update(ctx, taskId, body.toDetails());
    }

    @PatchMapping("/{taskId}/status")
    TaskResponse setStatus(BusinessContext ctx, @PathVariable long taskId, @Valid @RequestBody StatusBody body) {
        return tasks.setStatus(ctx, taskId, body.status());
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(BusinessContext ctx, @PathVariable long taskId) {
        tasks.delete(ctx, taskId);
    }
}
