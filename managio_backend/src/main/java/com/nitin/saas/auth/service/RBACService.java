package com.nitin.saas.auth.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.security.MemberPrincipal;
import com.nitin.saas.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Role-Based Access Control helper.
 *
 * FIX CVL-004: handles both UserPrincipal (owner/staff tokens) and
 * MemberPrincipal (member tokens) from the SecurityContext.
 *
 * getCurrentUserId() returns:
 *   - UserPrincipal  → User.id
 *   - MemberPrincipal → Member.id  (for member-scoped operations)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RBACService {

    private final UserRepository userRepository;

    // ── Principal access ──────────────────────────────────────────────────────

    /**
     * Returns the UserPrincipal if the current token is a user/staff token.
     * Returns null for member tokens or unauthenticated requests.
     */
    public UserPrincipal getCurrentUser() {
        Object principal = getPrincipal();
        if (principal instanceof UserPrincipal up) return up;
        return null;
    }

    /**
     * Returns the MemberPrincipal if the current token is a member token.
     * Returns null otherwise.
     */
    public MemberPrincipal getCurrentMember() {
        Object principal = getPrincipal();
        if (principal instanceof MemberPrincipal mp) return mp;
        return null;
    }

    /**
     * Returns the primary numeric ID for the current principal:
     *   - UserPrincipal  → User.id
     *   - MemberPrincipal → Member.id
     *
     * Throws AccessDeniedException if not authenticated.
     */
    public Long getCurrentUserId() {
        Object principal = getPrincipal();

        if (principal instanceof UserPrincipal up)   return up.getId();
        if (principal instanceof MemberPrincipal mp) return mp.getMemberId();

        throw new AccessDeniedException("User not authenticated");
    }

    /**
     * Returns true if the current principal is a member (not a user/staff).
     */
    public boolean isMemberPrincipal() {
        return getPrincipal() instanceof MemberPrincipal;
    }

    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    // ── Role / permission checks (user/staff tokens only) ────────────────────

    public boolean hasRole(Role role) {
        UserPrincipal principal = getCurrentUser();
        return principal != null && principal.getRoles().contains(role);
    }

    public boolean hasAnyRole(Role... roles) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;
        for (Role role : roles) {
            if (principal.getRoles().contains(role)) return true;
        }
        return false;
    }

    public boolean hasPermission(Role.Permission permission) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;
        return principal.getRoles().stream()
                .anyMatch(r -> r.hasPermission(permission));
    }

    public boolean canAccessResource(Long resourceOwnerId) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;
        if (hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)) return true;
        return principal.getId().equals(resourceOwnerId);
    }

    public void requireRole(Role role) {
        if (!hasRole(role)) {
            log.warn("Access denied — required role: {}", role);
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    public void requirePermission(Role.Permission permission) {
        if (!hasPermission(permission)) {
            log.warn("Access denied — required permission: {}", permission);
            throw new AccessDeniedException("Insufficient permissions");
        }
    }

    // ── User entity lookup (user tokens only) ────────────────────────────────

    public User getCurrentUserEntity() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Set<Role> getCurrentUserRoles() {
        UserPrincipal principal = getCurrentUser();
        return principal != null ? principal.getRoles() : Set.of();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private Object getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getPrincipal();
    }
}