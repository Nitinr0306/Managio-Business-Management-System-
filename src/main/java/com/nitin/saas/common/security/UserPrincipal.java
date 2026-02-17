package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private Set<Role> roles;
    private Boolean enabled;
    private Boolean accountLocked;
    private Boolean emailVerified;
    private Boolean accountNonExpired;
    private Boolean credentialsNonExpired;

    public static UserPrincipal create(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRoles())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .emailVerified(user.getEmailVerified())
                .accountNonExpired(user.isAccountNonExpired())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .build();
    }

    // ------------------------
    // Explicit Getters
    // ------------------------

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getAccountLocked() {
        return accountLocked;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    // ------------------------
    // Spring Security Methods
    // ------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        authorities.addAll(
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toSet())
        );

        roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .forEach(permission ->
                        authorities.add(
                                new SimpleGrantedAuthority("PERMISSION_" + permission.name())
                        )
                );

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired != null ? accountNonExpired : true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountLocked == null ? true : !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired != null ? credentialsNonExpired : true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : false;
    }

    // ------------------------
    // Utility Methods
    // ------------------------

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(Role.Permission permission) {
        return roles != null &&
                roles.stream().anyMatch(role -> role.hasPermission(permission));
    }
}
