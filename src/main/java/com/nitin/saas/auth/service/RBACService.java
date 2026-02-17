package com.nitin.saas.auth.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RBACService {

    private final UserRepository userRepository;

    public boolean hasRole(Role role) {
        UserPrincipal principal = getCurrentUser();
        return principal != null && principal.getRoles().contains(role);
    }

    public boolean hasAnyRole(Role... roles) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) {
            return false;
        }
        for (Role role : roles) {
            if (principal.getRoles().contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllRoles(Role... roles) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) {
            return false;
        }
        for (Role role : roles) {
            if (!principal.getRoles().contains(role)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasPermission(Role.Permission permission) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) {
            return false;
        }
        return principal.getRoles().stream()
                .anyMatch(role -> role.hasPermission(permission));
    }

    public boolean hasAnyPermission(Role.Permission... permissions) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) {
            return false;
        }
        for (Role.Permission permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAccessResource(Long resourceOwnerId) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) {
            return false;
        }

        if (hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)) {
            return true;
        }

        return principal.getId().equals(resourceOwnerId);
    }

    public void requireRole(Role role) {
        if (!hasRole(role)) {
            log.warn("Access denied - required role: {}", role);
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    public void requireAnyRole(Role... roles) {
        if (!hasAnyRole(roles)) {
            log.warn("Access denied - required any of roles: {}", (Object[]) roles);
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    public void requirePermission(Role.Permission permission) {
        if (!hasPermission(permission)) {
            log.warn("Access denied - required permission: {}", permission);
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    public void requireResourceAccess(Long resourceOwnerId) {
        if (!canAccessResource(resourceOwnerId)) {
            log.warn("Access denied - cannot access resource owned by: {}", resourceOwnerId);
            throw new AccessDeniedException("Access denied to this resource");
        }
    }

    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    public Long getCurrentUserId() {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        return principal.getId();
    }

    public User getCurrentUserEntity() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser");
    }

    public Set<Role> getCurrentUserRoles() {
        UserPrincipal principal = getCurrentUser();
        return principal != null ? principal.getRoles() : Set.of();
    }
}