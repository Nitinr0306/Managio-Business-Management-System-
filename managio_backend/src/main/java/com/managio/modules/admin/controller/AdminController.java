package com.managio.modules.admin.controller;

import com.managio.modules.admin.dto.AdminDtos.PlatformHealth;
import com.managio.modules.admin.dto.AdminDtos.PlatformOverview;
import com.managio.modules.admin.dto.AdminDtos.PlatformUserRow;
import com.managio.modules.admin.dto.AdminDtos.TenantRow;
import com.managio.modules.admin.repository.AdminQueries.PeopleView;
import com.managio.modules.admin.repository.AdminQueries.TenantSort;
import com.managio.modules.admin.repository.AdminQueries.TenantView;
import com.managio.modules.admin.service.AdminService;
import com.managio.modules.business.domain.BusinessType;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The platform console (D-028): read-only, for platform administrators (checked by AdminAccessInterceptor). */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
class AdminController {

    private final AdminService admin;

    /** 204 for a platform administrator, 403 for anyone else: lets the console decide what to show. */
    @GetMapping("/access")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void access() {
        // The interceptor has already decided.
    }

    @GetMapping("/overview")
    PlatformOverview overview() {
        return admin.overview();
    }

    @GetMapping("/businesses")
    PageResponse<TenantRow> businesses(@RequestParam(defaultValue = "ALL") TenantView view,
                                       @RequestParam(required = false) BusinessType type,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "NEWEST") TenantSort sort,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return admin.tenants(view, type, q, sort, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/people")
    PageResponse<PlatformUserRow> people(@RequestParam(defaultValue = "ALL") PeopleView view,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return admin.people(view, q, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/health")
    PlatformHealth health() {
        return admin.health();
    }
}
