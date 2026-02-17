package com.nitin.saas.staff.enums;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum StaffRole {
    OWNER(Set.of(
            Permission.ALL_PERMISSIONS
    )),

    MANAGER(Set.of(
            Permission.VIEW_MEMBERS,
            Permission.ADD_MEMBERS,
            Permission.EDIT_MEMBERS,
            Permission.VIEW_SUBSCRIPTIONS,
            Permission.ASSIGN_SUBSCRIPTIONS,
            Permission.CANCEL_SUBSCRIPTIONS,
            Permission.VIEW_PAYMENTS,
            Permission.RECORD_PAYMENTS,
            Permission.VIEW_REPORTS,
            Permission.EXPORT_DATA,
            Permission.VIEW_STAFF,
            Permission.VIEW_AUDIT_LOGS
    )),

    RECEPTIONIST(Set.of(
            Permission.VIEW_MEMBERS,
            Permission.ADD_MEMBERS,
            Permission.EDIT_MEMBERS,
            Permission.VIEW_SUBSCRIPTIONS,
            Permission.ASSIGN_SUBSCRIPTIONS,
            Permission.VIEW_PAYMENTS,
            Permission.RECORD_PAYMENTS
    )),

    TRAINER(Set.of(
            Permission.VIEW_MEMBERS,
            Permission.EDIT_MEMBER_NOTES,
            Permission.VIEW_SUBSCRIPTIONS,
            Permission.RECORD_ATTENDANCE
    )),

    ACCOUNTANT(Set.of(
            Permission.VIEW_MEMBERS,
            Permission.VIEW_SUBSCRIPTIONS,
            Permission.VIEW_PAYMENTS,
            Permission.RECORD_PAYMENTS,
            Permission.VIEW_REPORTS,
            Permission.EXPORT_DATA
    )),

    SALES(Set.of(
            Permission.VIEW_MEMBERS,
            Permission.ADD_MEMBERS,
            Permission.EDIT_MEMBERS,
            Permission.VIEW_SUBSCRIPTIONS,
            Permission.ASSIGN_SUBSCRIPTIONS,
            Permission.VIEW_PAYMENTS
    ));

    private final Set<Permission> permissions;

    StaffRole(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(Permission.ALL_PERMISSIONS) ||
                permissions.contains(permission);
    }

    public String getDisplayName() {
        return name().substring(0, 1).toUpperCase() +
                name().substring(1).toLowerCase().replace("_", " ");
    }

    public enum Permission {
        // Super permission
        ALL_PERMISSIONS,

        // Member Management
        VIEW_MEMBERS,
        ADD_MEMBERS,
        EDIT_MEMBERS,
        DELETE_MEMBERS,
        EDIT_MEMBER_NOTES,
        IMPORT_MEMBERS,
        EXPORT_MEMBERS,

        // Subscription Management
        VIEW_SUBSCRIPTIONS,
        ASSIGN_SUBSCRIPTIONS,
        CANCEL_SUBSCRIPTIONS,
        EXTEND_SUBSCRIPTIONS,
        VIEW_SUBSCRIPTION_HISTORY,

        // Payment Management
        VIEW_PAYMENTS,
        RECORD_PAYMENTS,
        REFUND_PAYMENTS,
        VIEW_PAYMENT_HISTORY,
        EXPORT_PAYMENTS,

        // Reports & Analytics
        VIEW_REPORTS,
        VIEW_DASHBOARD,
        EXPORT_DATA,
        VIEW_BUSINESS_STATS,

        // Staff Management
        VIEW_STAFF,
        ADD_STAFF,
        EDIT_STAFF,
        REMOVE_STAFF,
        VIEW_AUDIT_LOGS,

        // Attendance
        RECORD_ATTENDANCE,
        VIEW_ATTENDANCE,

        // Business Settings
        MANAGE_BUSINESS_SETTINGS,
        MANAGE_SUBSCRIPTION_PLANS,

        // System
        ACCESS_API;

        public String getDisplayName() {
            return name().replace("_", " ").toLowerCase();
        }
    }
}