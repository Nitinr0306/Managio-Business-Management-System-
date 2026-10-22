package com.managio.modules.business.controller;

import com.managio.modules.business.domain.BusinessType;
import com.managio.modules.business.service.BusinessOnboarding;
import com.managio.modules.business.service.BusinessService;
import com.managio.modules.business.dto.BusinessDtos.BusinessResponse;
import com.managio.modules.business.dto.BusinessDtos.BusinessTypeResponse;
import com.managio.modules.business.dto.BusinessDtos.CreateBusinessRequest;
import com.managio.modules.business.dto.BusinessDtos.MyBusinessResponse;
import com.managio.modules.business.dto.BusinessDtos.PermissionResponse;
import com.managio.modules.business.dto.BusinessDtos.UpdateBusinessRequest;
import com.managio.modules.business.dto.BusinessDtos.UpdateSettingsRequest;
import com.managio.platform.security.Principals.UserPrincipal;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
class BusinessController {

    private final BusinessOnboarding onboarding;
    private final BusinessService businesses;

    @PostMapping("/api/v1/businesses")
    @ResponseStatus(HttpStatus.CREATED)
    BusinessResponse create(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody CreateBusinessRequest request) {
        return onboarding.create(user.id(), request);
    }

    @GetMapping("/api/v1/me/businesses")
    List<MyBusinessResponse> mine(@AuthenticationPrincipal UserPrincipal user) {
        return businesses.mine(user.id());
    }

    @GetMapping("/api/v1/businesses/{businessId}")
    BusinessResponse get(BusinessContext ctx) {
        return businesses.describe(ctx.businessId());
    }

    @PatchMapping("/api/v1/businesses/{businessId}")
    @RequirePermission(Permission.SETTINGS_MANAGE)
    BusinessResponse update(BusinessContext ctx, @Valid @RequestBody UpdateBusinessRequest request) {
        return businesses.updateProfile(ctx, request);
    }

    @PutMapping("/api/v1/businesses/{businessId}/settings")
    @RequirePermission(Permission.SETTINGS_MANAGE)
    BusinessResponse updateSettings(BusinessContext ctx, @Valid @RequestBody UpdateSettingsRequest request) {
        return businesses.updateSettings(ctx, request);
    }

    @PostMapping("/api/v1/businesses/{businessId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(BusinessContext ctx) {
        businesses.archive(ctx);
    }

    @GetMapping("/api/v1/reference/business-types")
    List<BusinessTypeResponse> businessTypes() {
        return Arrays.stream(BusinessType.values())
                .map(t -> new BusinessTypeResponse(t, t.label(), t.labels(), t.defaultModules()))
                .toList();
    }

    @GetMapping("/api/v1/reference/permissions")
    List<PermissionResponse> permissions() {
        return Arrays.stream(Permission.values())
                .map(p -> new PermissionResponse(p.key(),
                        Character.toUpperCase(p.action().charAt(0)) + p.action().substring(1), p.action()))
                .toList();
    }
}
