package com.managio.modules.activity.controller;

import com.managio.modules.activity.service.ActivityLog;
import com.managio.modules.activity.service.ActivityLog.ActivityResponse;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/activity")
@RequiredArgsConstructor
class ActivityController {

    private final ActivityLog activityLog;

    @GetMapping
    @RequirePermission(Permission.ACTIVITY_VIEW)
    PageResponse<ActivityResponse> list(BusinessContext ctx,
                                        @RequestParam(required = false) String entityType,
                                        @RequestParam(required = false) Long entityId,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return activityLog.list(ctx.businessId(), entityType, entityId, PageResponse.request(page, size, Sort.unsorted()));
    }
}
