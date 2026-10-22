package com.managio.modules.attendance.controller;

import com.managio.modules.attendance.service.AttendanceService;
import com.managio.modules.attendance.service.GroupService;
import com.managio.modules.attendance.service.AttendanceService.CheckIn;
import com.managio.modules.attendance.service.GroupService.GroupResponse;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequireModule;
import com.managio.platform.tenancy.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
@RequiredArgsConstructor
class GroupAttendanceController {

    private final GroupService groups;
    private final AttendanceService attendance;
    private final Clock clock;

    record GroupRequest(@NotBlank(message = "Enter a name") @Size(max = 80, message = "Use 80 characters or fewer") String name,
                        @Size(max = 300) String description) {
    }

    record MembersRequest(@NotNull Set<Long> customerIds) {
    }

    record CheckInRequest(@NotNull Long customerId) {
    }

    record RollCallRequest(@NotNull LocalDate date, @NotNull Set<Long> present) {
    }

    // ── Groups (GROUPS module) ───────────────────────────────────────────────

    @GetMapping("/groups")
    @RequireModule(Module.GROUPS)
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    List<GroupResponse> groups(BusinessContext ctx) {
        return groups.list(ctx.businessId());
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModule(Module.GROUPS)
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    GroupResponse createGroup(BusinessContext ctx, @Valid @RequestBody GroupRequest request) {
        return groups.create(ctx, request.name(), request.description());
    }

    @PatchMapping("/groups/{groupId}")
    @RequireModule(Module.GROUPS)
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    GroupResponse updateGroup(BusinessContext ctx, @PathVariable long groupId, @Valid @RequestBody GroupRequest request) {
        return groups.update(ctx, groupId, request.name(), request.description());
    }

    @DeleteMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModule(Module.GROUPS)
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    void deleteGroup(BusinessContext ctx, @PathVariable long groupId) {
        groups.delete(ctx, groupId);
    }

    @PostMapping("/groups/{groupId}/members")
    @RequireModule(Module.GROUPS)
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    GroupResponse addMembers(BusinessContext ctx, @PathVariable long groupId, @Valid @RequestBody MembersRequest request) {
        return groups.addMembers(ctx, groupId, request.customerIds());
    }

    @DeleteMapping("/groups/{groupId}/members/{customerId}")
    @RequireModule(Module.GROUPS)
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    GroupResponse removeMember(BusinessContext ctx, @PathVariable long groupId, @PathVariable long customerId) {
        return groups.removeMember(ctx, groupId, customerId);
    }

    // ── Attendance (ATTENDANCE module) ───────────────────────────────────────

    @GetMapping("/attendance")
    @RequireModule(Module.ATTENDANCE)
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    List<CheckIn> attendance(BusinessContext ctx,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) Long groupId) {
        return attendance.forDay(ctx.businessId(), date != null ? date : ctx.today(clock), groupId);
    }

    @PostMapping("/attendance")
    @RequireModule(Module.ATTENDANCE)
    @RequirePermission(Permission.ATTENDANCE_MARK)
    CheckIn checkIn(BusinessContext ctx, @Valid @RequestBody CheckInRequest request) {
        return attendance.checkIn(ctx, request.customerId());
    }

    @PostMapping("/groups/{groupId}/attendance")
    @RequireModule(Module.ATTENDANCE)
    @RequirePermission(Permission.ATTENDANCE_MARK)
    List<CheckIn> rollCall(BusinessContext ctx, @PathVariable long groupId, @Valid @RequestBody RollCallRequest request) {
        return attendance.markGroup(ctx, groupId, request.date(), request.present());
    }

    @DeleteMapping("/attendance/{attendanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModule(Module.ATTENDANCE)
    @RequirePermission(Permission.ATTENDANCE_MARK)
    void undoCheckIn(BusinessContext ctx, @PathVariable long attendanceId) {
        attendance.undo(ctx, attendanceId);
    }
}
