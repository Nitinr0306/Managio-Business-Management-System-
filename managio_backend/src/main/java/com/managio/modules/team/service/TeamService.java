package com.managio.modules.team.service;

import com.managio.modules.team.domain.TeamMember;
import com.managio.modules.team.repository.TeamMemberRepository;
import com.managio.modules.team.domain.Role;

import com.managio.platform.persistence.Persistence;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** The people who work in a business (TEAM-3, TEAM-4, TEAM-7, TEAM-8, TEAM-9). */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamMemberRepository members;
    private final RoleService roles;
    private final JdbcClient jdbc;
    private final ActivityLog activity;
    private final Persistence persistence;
    private final Clock clock;

    public record TeamMemberResponse(long id, long userId, String fullName, String email, long roleId, String roleName,
                                     boolean owner, String status, @Nullable String title,
                                     @Nullable String employeeCode, @Nullable String phone,
                                     @Nullable LocalDate joinedOn, @Nullable LocalDate leftOn,
                                     @Nullable BigDecimal monthlySalary, @Nullable String notes) {
    }

    public record UpdateMember(Long roleId, String title, String employeeCode, String phone, LocalDate joinedOn,
                               String notes, BigDecimal monthlySalary, boolean clearSalary) {
    }

    /** Makes the creator of a business its owner (BUS-1). */
    @Transactional
    public void addOwner(long businessId, long userId, long ownerRoleId, LocalDate joinedOn) {
        members.save(TeamMember.join(businessId, userId, ownerRoleId, null, joinedOn));
    }

    /** Adds or re-activates someone accepting an invitation. */
    @Transactional
    public TeamMember join(long businessId, long userId, long roleId, String title, LocalDate joinedOn) {
        return members.findByBusinessIdAndUserId(businessId, userId)
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.rejoin(roleId, title, joinedOn);
                    }
                    return existing;
                })
                .orElseGet(() -> members.save(TeamMember.join(businessId, userId, roleId, title, joinedOn)));
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> list(BusinessContext ctx, boolean includeLeft) {
        return jdbc.sql(SELECT_MEMBERS + " WHERE tm.business_id = :businessId"
                        + (includeLeft ? "" : " AND tm.status <> 'LEFT'")
                        + " ORDER BY (r.system_key = 'OWNER') DESC, tm.status, lower(u.full_name)")
                .param("businessId", ctx.businessId())
                .query((rs, i) -> map(rs, ctx.can(Permission.PAYROLL_MANAGE)))
                .list();
    }

    @Transactional(readOnly = true)
    public TeamMemberResponse get(BusinessContext ctx, long memberId) {
        persistence.flush();
        return jdbc.sql(SELECT_MEMBERS + " WHERE tm.business_id = :businessId AND tm.id = :id")
                .param("businessId", ctx.businessId())
                .param("id", memberId)
                .query((rs, i) -> map(rs, ctx.can(Permission.PAYROLL_MANAGE)))
                .optional()
                .orElseThrow(() -> ApiException.notFound("Team member"));
    }

    @Transactional
    public TeamMemberResponse update(BusinessContext ctx, long memberId, UpdateMember update) {
        TeamMember member = load(ctx, memberId);
        if (update.roleId() != null && update.roleId() != member.getRoleId()) {
            guardAgainstOwnerAndSelf(ctx, member, "change the role of");
            Role role = roles.assignable(ctx, update.roleId());
            member.changeRole(role.getId());
            activity.record(ctx, "TEAM_ROLE_CHANGED", "TEAM_MEMBER", memberId, "Changed role to " + role.getName());
        }
        member.updateDetails(Normalize.text(update.title()), Normalize.text(update.employeeCode()),
                Normalize.phone(update.phone()), update.joinedOn(), Normalize.text(update.notes()));
        if (update.monthlySalary() != null || update.clearSalary()) {
            ctx.require(Permission.PAYROLL_MANAGE);
            member.setMonthlySalary(update.clearSalary() ? null : update.monthlySalary());
            activity.record(ctx, "TEAM_SALARY_CHANGED", "TEAM_MEMBER", memberId, "Changed monthly salary");
        }
        return get(ctx, memberId);
    }

    @Transactional
    public TeamMemberResponse suspend(BusinessContext ctx, long memberId) {
        TeamMember member = load(ctx, memberId);
        guardAgainstOwnerAndSelf(ctx, member, "suspend");
        member.suspend();
        activity.record(ctx, "TEAM_SUSPENDED", "TEAM_MEMBER", memberId, "Suspended " + name(member));
        return get(ctx, memberId);
    }

    @Transactional
    public TeamMemberResponse reactivate(BusinessContext ctx, long memberId) {
        TeamMember member = load(ctx, memberId);
        guardAgainstOwnerAndSelf(ctx, member, "reactivate");
        if (member.getStatus() == TeamMember.Status.LEFT) {
            throw ApiException.rule("team.left", "This person left the business. Invite them again to bring them back.");
        }
        member.reactivate();
        activity.record(ctx, "TEAM_REACTIVATED", "TEAM_MEMBER", memberId, "Reactivated " + name(member));
        return get(ctx, memberId);
    }

    @Transactional
    public TeamMemberResponse remove(BusinessContext ctx, long memberId) {
        TeamMember member = load(ctx, memberId);
        guardAgainstOwnerAndSelf(ctx, member, "remove");
        member.leave(ctx.today(clock));
        activity.record(ctx, "TEAM_REMOVED", "TEAM_MEMBER", memberId, "Removed " + name(member) + " from the team");
        return get(ctx, memberId);
    }

    @Transactional(readOnly = true)
    public TeamMember load(BusinessContext ctx, long memberId) {
        return members.findByIdAndBusinessId(memberId, ctx.businessId())
                .orElseThrow(() -> ApiException.notFound("Team member"));
    }

    /** TEAM-3, TEAM-8: nobody acts on the owner's membership or on their own. */
    private void guardAgainstOwnerAndSelf(BusinessContext ctx, TeamMember member, String verb) {
        if (member.getId() == ctx.memberId()) {
            throw ApiException.rule("team.self", "You can't " + verb + " yourself.");
        }
        if (roles.get(ctx.businessId(), member.getRoleId()).isOwnerRole()) {
            throw ApiException.rule("team.owner", "You can't " + verb + " the owner.");
        }
    }

    private String name(TeamMember member) {
        return jdbc.sql("SELECT full_name FROM users WHERE id = :id").param("id", member.getUserId())
                .query(String.class).single();
    }

    private static final String SELECT_MEMBERS = """
            SELECT tm.*, u.full_name, u.email, r.name AS role_name, r.system_key
            FROM team_members tm
            JOIN users u ON u.id = tm.user_id
            JOIN roles r ON r.id = tm.role_id""";

    private static TeamMemberResponse map(ResultSet rs, boolean showSalary) throws SQLException {
        return new TeamMemberResponse(rs.getLong("id"), rs.getLong("user_id"), rs.getString("full_name"),
                rs.getString("email"), rs.getLong("role_id"), rs.getString("role_name"),
                Role.OWNER_KEY.equals(rs.getString("system_key")), rs.getString("status"), rs.getString("title"),
                rs.getString("employee_code"), rs.getString("phone"), rs.getObject("joined_on", LocalDate.class),
                rs.getObject("left_on", LocalDate.class), showSalary ? rs.getBigDecimal("monthly_salary") : null,
                rs.getString("notes"));
    }
}
