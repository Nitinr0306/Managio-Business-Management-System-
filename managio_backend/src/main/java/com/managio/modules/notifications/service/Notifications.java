package com.managio.modules.notifications.service;

import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** In-app notifications for a user (REQ-4). Written in the caller's transaction. */
@Service
@RequiredArgsConstructor
public class Notifications {

    private final JdbcClient jdbc;
    private final Clock clock;

    public record NotificationResponse(long id, @Nullable Long businessId, String kind, String title,
                                       @Nullable String body, @Nullable String link, boolean read, Instant createdAt) {
    }

    public void notifyUser(long userId, long businessId, String kind, String title, String body, String link) {
        jdbc.sql("""
                        INSERT INTO notifications (user_id, business_id, kind, title, body, link, created_at)
                        VALUES (:userId, :businessId, :kind, :title, :body, :link, :now)""")
                .param("userId", userId).param("businessId", businessId).param("kind", kind).param("title", clip(title, 200))
                .param("body", clip(body, 500)).param("link", link).param("now", Timestamp.from(clock.instant())).update();
    }

    /** Tells everyone who can approve requests in the business, except {@code exceptUserId}. */
    public void notifyApprovers(long businessId, long exceptUserId, String kind, String title, String body, String link) {
        jdbc.sql("""
                        INSERT INTO notifications (user_id, business_id, kind, title, body, link, created_at)
                        SELECT tm.user_id, :businessId, :kind, :title, :body, :link, :now
                        FROM team_members tm JOIN roles r ON r.id = tm.role_id
                        WHERE tm.business_id = :businessId AND tm.status = 'ACTIVE' AND tm.user_id <> :except
                          AND 'requests.approve' = ANY (r.permissions)""")
                .param("businessId", businessId).param("kind", kind).param("title", clip(title, 200))
                .param("body", clip(body, 500)).param("link", link).param("except", exceptUserId)
                .param("now", Timestamp.from(clock.instant())).update();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(long userId, boolean unreadOnly, int limit) {
        return jdbc.sql("""
                        SELECT id, business_id, kind, title, body, link, read_at, created_at FROM notifications
                        WHERE user_id = :userId""" + (unreadOnly ? " AND read_at IS NULL" : "")
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit")
                .param("userId", userId).param("limit", Math.clamp(limit, 1, 100))
                .query((rs, i) -> new NotificationResponse(rs.getLong("id"), (Long) rs.getObject("business_id"),
                        rs.getString("kind"), rs.getString("title"), rs.getString("body"), rs.getString("link"),
                        rs.getTimestamp("read_at") != null, rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return jdbc.sql("SELECT count(*) FROM notifications WHERE user_id = :userId AND read_at IS NULL")
                .param("userId", userId).query(Long.class).single();
    }

    /** Marks the given notifications read, or all of them when {@code ids} is empty. Only the owner's rows change. */
    @Transactional
    public void markRead(long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            jdbc.sql("UPDATE notifications SET read_at = :now WHERE user_id = :userId AND read_at IS NULL")
                    .param("now", Timestamp.from(clock.instant())).param("userId", userId).update();
        } else {
            jdbc.sql("UPDATE notifications SET read_at = :now WHERE user_id = :userId AND read_at IS NULL AND id IN (:ids)")
                    .param("now", Timestamp.from(clock.instant())).param("userId", userId).param("ids", ids).update();
        }
    }

    private static String clip(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
