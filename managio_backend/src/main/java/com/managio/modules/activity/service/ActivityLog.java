package com.managio.modules.activity.service;

import com.managio.modules.activity.domain.ActivityEvent;
import com.managio.modules.activity.repository.ActivityEventRepository;
import com.managio.modules.activity.domain.ActivityEvent.ActorType;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.web.Nullable;
import com.managio.platform.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;

/**
 * Records and reads activity events. Events are written in the caller's transaction, so an action that rolls
 * back leaves no trace in the log (docs/ARCHITECTURE.md#cross-cutting-behaviour).
 */
@Service
@RequiredArgsConstructor
public class ActivityLog {

    private final ActivityEventRepository events;
    private final JdbcClient jdbc;
    private final Clock clock;

    /** Records an action taken by the signed-in team member described by {@code ctx}. */
    public void record(BusinessContext ctx, String action, String entityType, Long entityId, String summary) {
        save(ctx.businessId(), ActorType.USER, ctx.userId(), userName(ctx.userId()), action, entityType, entityId, summary);
    }

    /**
     * Records an action taken by a user outside a business request, e.g. accepting an invitation.
     * Pass {@code userName} if known; otherwise it is looked up.
     */
    public void recordUser(long businessId, long userId, String userName, String action, String entityType,
                           Long entityId, String summary) {
        save(businessId, ActorType.USER, userId, userName != null ? userName : userName(userId), action, entityType,
                entityId, summary);
    }

    public void recordCustomer(long businessId, long customerId, String customerName, String action, String summary) {
        save(businessId, ActorType.CUSTOMER, customerId, customerName, action, "CUSTOMER", customerId, summary);
    }

    public void recordSystem(long businessId, String action, String entityType, Long entityId, String summary) {
        save(businessId, ActorType.SYSTEM, null, "Managio", action, entityType, entityId, summary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(long businessId, String entityType, Long entityId, Pageable pageable) {
        var page = entityType == null
                ? events.findByBusinessIdOrderByCreatedAtDescIdDesc(businessId, pageable)
                : entityId == null
                ? events.findByBusinessIdAndEntityTypeOrderByCreatedAtDescIdDesc(businessId, entityType, pageable)
                : events.findByBusinessIdAndEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(businessId, entityType, entityId, pageable);
        return PageResponse.of(page, ActivityResponse::from);
    }

    private String userName(long userId) {
        return jdbc.sql("SELECT full_name FROM users WHERE id = :id").param("id", userId)
                .query(String.class).optional().orElse(null);
    }

    private void save(long businessId, ActorType actorType, Long actorId, String actorName, String action,
                      String entityType, Long entityId, String summary) {
        events.save(new ActivityEvent(businessId, actorType, actorId, actorName, action, entityType, entityId, summary,
                clientIp(), Instant.now(clock)));
    }

    private static String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }

    public record ActivityResponse(long id, String actorType, @Nullable String actorName, String action,
                                   @Nullable String entityType, @Nullable Long entityId, String summary,
                                   Instant createdAt) {
        static ActivityResponse from(ActivityEvent e) {
            return new ActivityResponse(e.getId(), e.getActorType().name(), e.getActorName(), e.getAction(),
                    e.getEntityType(), e.getEntityId(), e.getSummary(), e.getCreatedAt());
        }
    }
}
