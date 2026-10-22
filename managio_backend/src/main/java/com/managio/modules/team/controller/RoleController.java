package com.managio.modules.team.controller;

import com.managio.modules.team.service.RoleService;
import com.managio.modules.business.dto.BusinessDtos.RoleRequest;
import com.managio.modules.team.service.RoleService.RoleResponse;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Roles of a business. Reading needs team.view; changes are owner-only (TEAM-6), enforced in the service. */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/roles")
@RequiredArgsConstructor
class RoleController {

    private final RoleService roles;

    @GetMapping
    @RequirePermission(Permission.TEAM_VIEW)
    List<RoleResponse> list(BusinessContext ctx) {
        return roles.list(ctx.businessId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RoleResponse create(BusinessContext ctx, @Valid @RequestBody RoleRequest request) {
        return roles.create(ctx, request.name(), request.description(), request.permissions());
    }

    @PatchMapping("/{roleId}")
    RoleResponse update(BusinessContext ctx, @PathVariable long roleId, @Valid @RequestBody RoleRequest request) {
        return roles.update(ctx, roleId, request.name(), request.description(), request.permissions());
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(BusinessContext ctx, @PathVariable long roleId) {
        roles.delete(ctx, roleId);
    }
}
