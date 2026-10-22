package com.managio.modules.attendance.service;

import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Groups of customers: batches, classes or any grouping a business uses. A plain join table, so this
 * service works with SQL directly instead of an entity relationship.
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final JdbcClient jdbc;
    private final ActivityLog activity;

    public record GroupResponse(long id, String name, @Nullable String description, long memberCount) {
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> list(long businessId) {
        return jdbc.sql("""
                        SELECT g.id, g.name, g.description,
                               (SELECT count(*) FROM customer_group_members gm JOIN customers c ON c.id = gm.customer_id
                                WHERE gm.group_id = g.id AND c.status = 'ACTIVE') AS member_count
                        FROM customer_groups g WHERE g.business_id = :businessId ORDER BY lower(g.name)""")
                .param("businessId", businessId)
                .query((rs, i) -> new GroupResponse(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                        rs.getLong("member_count")))
                .list();
    }

    @Transactional
    public GroupResponse create(BusinessContext ctx, String name, String description) {
        try {
            long id = jdbc.sql("""
                            INSERT INTO customer_groups (business_id, name, description) VALUES (:businessId, :name, :description)
                            RETURNING id""")
                    .param("businessId", ctx.businessId()).param("name", name.trim()).param("description", description)
                    .query(Long.class).single();
            activity.record(ctx, "GROUP_CREATED", "GROUP", id, "Created group " + name.trim());
            return get(ctx.businessId(), id);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("group.name_taken", "A group called " + name.trim() + " already exists.");
        }
    }

    @Transactional
    public GroupResponse update(BusinessContext ctx, long groupId, String name, String description) {
        get(ctx.businessId(), groupId);
        try {
            jdbc.sql("UPDATE customer_groups SET name = :name, description = :description WHERE id = :id")
                    .param("name", name.trim()).param("description", description).param("id", groupId).update();
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("group.name_taken", "A group called " + name.trim() + " already exists.");
        }
        return get(ctx.businessId(), groupId);
    }

    @Transactional
    public void delete(BusinessContext ctx, long groupId) {
        GroupResponse group = get(ctx.businessId(), groupId);
        jdbc.sql("DELETE FROM customer_groups WHERE id = :id").param("id", groupId).update();
        activity.record(ctx, "GROUP_DELETED", "GROUP", groupId, "Deleted group " + group.name());
    }

    /** Adds customers to a group; customers outside the business are rejected, existing members ignored. */
    @Transactional
    public GroupResponse addMembers(BusinessContext ctx, long groupId, Set<Long> customerIds) {
        get(ctx.businessId(), groupId);
        requireCustomersInBusiness(ctx.businessId(), customerIds);
        for (Long customerId : customerIds) {
            jdbc.sql("""
                            INSERT INTO customer_group_members (group_id, customer_id) VALUES (:groupId, :customerId)
                            ON CONFLICT DO NOTHING""")
                    .param("groupId", groupId).param("customerId", customerId).update();
        }
        return get(ctx.businessId(), groupId);
    }

    @Transactional
    public GroupResponse removeMember(BusinessContext ctx, long groupId, long customerId) {
        get(ctx.businessId(), groupId);
        jdbc.sql("DELETE FROM customer_group_members WHERE group_id = :groupId AND customer_id = :customerId")
                .param("groupId", groupId).param("customerId", customerId).update();
        return get(ctx.businessId(), groupId);
    }

    @Transactional(readOnly = true)
    public GroupResponse get(long businessId, long groupId) {
        return list(businessId).stream().filter(g -> g.id() == groupId).findFirst()
                .orElseThrow(() -> ApiException.notFound("Group"));
    }

    @Transactional(readOnly = true)
    public List<Long> activeMemberIds(long businessId, long groupId) {
        get(businessId, groupId);
        return jdbc.sql("""
                        SELECT gm.customer_id FROM customer_group_members gm JOIN customers c ON c.id = gm.customer_id
                        WHERE gm.group_id = :groupId AND c.status = 'ACTIVE'""")
                .param("groupId", groupId).query(Long.class).list();
    }

    private void requireCustomersInBusiness(long businessId, Set<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return;
        }
        long found = jdbc.sql("SELECT count(*) FROM customers WHERE business_id = :businessId AND id IN (:ids)")
                .param("businessId", businessId).param("ids", customerIds).query(Long.class).single();
        if (found != customerIds.size()) {
            throw ApiException.notFound("Customer");
        }
    }
}
