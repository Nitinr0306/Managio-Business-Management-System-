package com.nitin.saas.auth.enums;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Role {
    USER(Set.of(
            Permission.READ_OWN_PROFILE,
            Permission.UPDATE_OWN_PROFILE,
            Permission.READ_OWN_DATA)),
    ADMIN(Set.of(
            Permission.READ_OWN_PROFILE, Permission.UPDATE_OWN_PROFILE,
            Permission.READ_OWN_DATA,
            Permission.READ_ALL_USERS, Permission.UPDATE_ALL_USERS,
            Permission.READ_ALL_DATA, Permission.UPDATE_ALL_DATA,
            Permission.MANAGE_BUSINESS)),
    SUPER_ADMIN(Stream.of(Permission.values()).collect(Collectors.toSet()));

    private final Set<Permission> permissions;
    Role(Set<Permission> p) { this.permissions = p; }
    public Set<Permission> getPermissions() { return permissions; }
    public boolean hasPermission(Permission p) { return permissions.contains(p); }

    public enum Permission {
        READ_OWN_PROFILE, UPDATE_OWN_PROFILE, DELETE_OWN_PROFILE,
        READ_OWN_DATA, UPDATE_OWN_DATA, DELETE_OWN_DATA,
        READ_ALL_USERS, UPDATE_ALL_USERS, DELETE_ALL_USERS, CREATE_USERS,
        READ_ALL_DATA, UPDATE_ALL_DATA, DELETE_ALL_DATA,
        MANAGE_BUSINESS, MANAGE_SUBSCRIPTIONS, MANAGE_PAYMENTS,
        VIEW_ANALYTICS, VIEW_AUDIT_LOGS, SYSTEM_ADMIN
    }
}