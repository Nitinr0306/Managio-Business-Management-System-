package com.managio.modules.notifications.controller;

import com.managio.modules.notifications.service.Notifications;
import com.managio.modules.notifications.service.Notifications.NotificationResponse;
import com.managio.platform.security.Principals.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** The signed-in user's notifications across every business they belong to. */
@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
class NotificationController {

    private final Notifications notifications;

    record ReadBody(List<Long> ids) {
    }

    @GetMapping
    List<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal user,
                                    @RequestParam(defaultValue = "false") boolean unread,
                                    @RequestParam(defaultValue = "30") int limit) {
        return notifications.list(user.id(), unread, limit);
    }

    @GetMapping("/count")
    Map<String, Long> count(@AuthenticationPrincipal UserPrincipal user) {
        return Map.of("unread", notifications.unreadCount(user.id()));
    }

    /** An empty or missing {@code ids} marks everything read. */
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@AuthenticationPrincipal UserPrincipal user, @RequestBody(required = false) ReadBody body) {
        notifications.markRead(user.id(), body == null ? null : body.ids());
    }
}
