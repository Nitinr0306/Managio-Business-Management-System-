package com.managio.modules.team.controller;

import com.managio.modules.team.service.InvitationService;
import com.managio.modules.team.service.TeamService;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.modules.team.service.InvitationService.InvitationResponse;
import com.managio.modules.team.service.TeamService.TeamMemberResponse;
import com.managio.modules.team.service.TeamService.UpdateMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
@RequiredArgsConstructor
class TeamController {

    private final TeamService team;
    private final InvitationService invitations;

    /** Saves the edit form; omitted role or joining date means "unchanged". Salary needs payroll.manage (TEAM-9). */
    record UpdateMemberRequest(Long roleId, @Size(max = 100) String title, @Size(max = 20) String employeeCode,
                               @Size(max = 20) String phone, LocalDate joinedOn, @Size(max = 2000) String notes,
                               @DecimalMin(value = "0", message = "Salary can't be negative") BigDecimal monthlySalary,
                               boolean clearSalary) {
    }

    record InviteRequest(@NotBlank(message = "Enter an email") @Email(message = "Enter a valid email") String email,
                         @NotNull(message = "Choose a role") Long roleId,
                         @Size(max = 100, message = "Use 100 characters or fewer") String title) {
    }

    @GetMapping("/team")
    @RequirePermission(Permission.TEAM_VIEW)
    List<TeamMemberResponse> list(BusinessContext ctx, @RequestParam(defaultValue = "false") boolean includeLeft) {
        return team.list(ctx, includeLeft);
    }

    @GetMapping("/team/{memberId}")
    @RequirePermission(Permission.TEAM_VIEW)
    TeamMemberResponse get(BusinessContext ctx, @PathVariable long memberId) {
        return team.get(ctx, memberId);
    }

    @PatchMapping("/team/{memberId}")
    @RequirePermission(Permission.TEAM_MANAGE)
    TeamMemberResponse update(BusinessContext ctx, @PathVariable long memberId, @Valid @RequestBody UpdateMemberRequest r) {
        return team.update(ctx, memberId, new UpdateMember(r.roleId(), r.title(), r.employeeCode(), r.phone(),
                r.joinedOn(), r.notes(), r.monthlySalary(), r.clearSalary()));
    }

    @PostMapping("/team/{memberId}/suspend")
    @RequirePermission(Permission.TEAM_MANAGE)
    TeamMemberResponse suspend(BusinessContext ctx, @PathVariable long memberId) {
        return team.suspend(ctx, memberId);
    }

    @PostMapping("/team/{memberId}/reactivate")
    @RequirePermission(Permission.TEAM_MANAGE)
    TeamMemberResponse reactivate(BusinessContext ctx, @PathVariable long memberId) {
        return team.reactivate(ctx, memberId);
    }

    @PostMapping("/team/{memberId}/remove")
    @RequirePermission(Permission.TEAM_MANAGE)
    TeamMemberResponse remove(BusinessContext ctx, @PathVariable long memberId) {
        return team.remove(ctx, memberId);
    }

    @GetMapping("/invitations")
    @RequirePermission(Permission.TEAM_MANAGE)
    List<InvitationResponse> pendingInvitations(BusinessContext ctx) {
        return invitations.pending(ctx);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.TEAM_MANAGE)
    InvitationResponse invite(BusinessContext ctx, @Valid @RequestBody InviteRequest request) {
        return invitations.invite(ctx, request.email(), request.roleId(), request.title());
    }

    @PostMapping("/invitations/{invitationId}/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(Permission.TEAM_MANAGE)
    void resend(BusinessContext ctx, @PathVariable long invitationId) {
        invitations.resend(ctx, invitationId);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(Permission.TEAM_MANAGE)
    void revoke(BusinessContext ctx, @PathVariable long invitationId) {
        invitations.revoke(ctx, invitationId);
    }
}
