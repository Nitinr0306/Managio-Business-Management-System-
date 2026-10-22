package com.managio.modules.team.service;

import com.managio.modules.team.domain.Role;
import com.managio.modules.team.repository.RoleRepository;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Roles and the rules for granting them (TEAM-5, TEAM-6, TEAM-7). */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roles;
    private final JdbcClient jdbc;
    private final ActivityLog activity;

    public record RoleResponse(long id, String name, @Nullable String description, List<String> permissions,
                               @Nullable String systemKey, boolean owner, long memberCount) {
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list(long businessId) {
        Map<Long, Long> counts = jdbc.sql("""
                        SELECT role_id, count(*) AS n FROM team_members
                        WHERE business_id = :businessId AND status <> 'LEFT' GROUP BY role_id""")
                .param("businessId", businessId)
                .query((rs, i) -> Map.entry(rs.getLong("role_id"), rs.getLong("n")))
                .list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return roles.findByBusinessIdOrderByIdAsc(businessId).stream()
                .map(r -> toResponse(r, counts.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    @Transactional
    public RoleResponse create(BusinessContext ctx, String name, String description, Set<Permission> permissions) {
        requireOwner(ctx);
        String cleanName = name.trim();
        if (roles.nameTaken(ctx.businessId(), cleanName, null)) {
            throw ApiException.conflict("role.name_taken", "A role called " + cleanName + " already exists.");
        }
        Role role = roles.save(Role.of(ctx.businessId(), cleanName, description, permissions, null));
        activity.record(ctx, "ROLE_CREATED", "ROLE", role.getId(), "Created role " + cleanName);
        return toResponse(role, 0);
    }

    @Transactional
    public RoleResponse update(BusinessContext ctx, long roleId, String name, String description, Set<Permission> permissions) {
        requireOwner(ctx);
        Role role = get(ctx.businessId(), roleId);
        if (role.isOwnerRole()) {
            throw ApiException.rule("role.owner_locked", "The Owner role always has every permission and can't be changed.");
        }
        String cleanName = name.trim();
        if (roles.nameTaken(ctx.businessId(), cleanName, roleId)) {
            throw ApiException.conflict("role.name_taken", "A role called " + cleanName + " already exists.");
        }
        role.update(cleanName, description, permissions);
        activity.record(ctx, "ROLE_UPDATED", "ROLE", roleId, "Updated role " + cleanName);
        return toResponse(role, memberCount(roleId));
    }

    @Transactional
    public void delete(BusinessContext ctx, long roleId) {
        requireOwner(ctx);
        Role role = get(ctx.businessId(), roleId);
        if (role.isOwnerRole()) {
            throw ApiException.rule("role.owner_locked", "The Owner role can't be deleted.");
        }
        boolean referenced = jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM team_members WHERE role_id = :id)
                            OR EXISTS (SELECT 1 FROM invitations WHERE role_id = :id
                                       AND accepted_at IS NULL AND revoked_at IS NULL)""")
                .param("id", roleId).query(Boolean.class).single();
        if (referenced) {
            throw ApiException.rule("role.in_use",
                    "People or pending invitations use this role. Move them to another role first.");
        }
        roles.delete(role);
        activity.record(ctx, "ROLE_DELETED", "ROLE", roleId, "Deleted role " + role.getName());
    }

    /** TEAM-7: the caller may only hand out a role whose permissions they all hold. */
    @Transactional(readOnly = true)
    public Role assignable(BusinessContext ctx, long roleId) {
        Role role = get(ctx.businessId(), roleId);
        if (role.isOwnerRole()) {
            throw ApiException.rule("role.owner_not_assignable", "The Owner role can't be given to someone else.");
        }
        Set<Permission> missing = role.permissionSet();
        missing.removeAll(ctx.permissions());
        if (!missing.isEmpty()) {
            throw ApiException.forbidden("You can only give people a role whose permissions you have yourself.");
        }
        return role;
    }

    @Transactional(readOnly = true)
    public Role get(long businessId, long roleId) {
        return roles.findByIdAndBusinessId(roleId, businessId).orElseThrow(() -> ApiException.notFound("Role"));
    }

    private long memberCount(long roleId) {
        return jdbc.sql("SELECT count(*) FROM team_members WHERE role_id = :id AND status <> 'LEFT'")
                .param("id", roleId).query(Long.class).single();
    }

    private static void requireOwner(BusinessContext ctx) {
        if (!ctx.owner()) {
            throw ApiException.forbidden("Only the owner can change roles.");
        }
    }

    private static RoleResponse toResponse(Role role, long memberCount) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), List.of(role.getPermissions()),
                role.getSystemKey(), role.isOwnerRole(), memberCount);
    }
}
