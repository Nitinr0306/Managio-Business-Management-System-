package com.nitin.saas.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * SecurityContext principal for member tokens.
 *
 * Member tokens are issued by MemberAuthService and carry memberId + businessId
 * claims instead of userId + roles.  JwtAuthFilter creates this object when it
 * detects userType=member in the token claims.
 *
 * RBACService.getCurrentUserId() returns memberId for members.
 * Endpoints that are open to both users and members should check getAuthorities()
 * for ROLE_MEMBER when they need to differentiate behaviour.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPrincipal implements UserDetails {

    private Long   memberId;
    private Long   businessId;
    private String phone;
    private String email;
    private String fullName;

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }

    /** Members do not store their password in the principal. */
    @Override
    public String getPassword() { return null; }

    /** Username is the phone or email, whichever is set. */
    @Override
    public String getUsername() {
        return phone != null ? phone : email;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Convenience method so that code which calls rbacService.getCurrentUserId()
     * receives the memberId in a member-authenticated context.
     */
    public Long getId() {
        return memberId;
    }
}