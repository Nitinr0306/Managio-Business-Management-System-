package com.managio.platform.tenancy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The permission catalogue (docs/PRODUCT.md#permissions-and-default-roles). Keys are stored in {@code roles.permissions}. */
public enum Permission {
    CUSTOMERS_VIEW("customers.view", "see customers"),
    CUSTOMERS_MANAGE("customers.manage", "add or edit customers"),
    CUSTOMERS_ARCHIVE("customers.archive", "archive customers"),
    CUSTOMERS_IMPORT("customers.import", "import or export customers"),
    ATTENDANCE_MARK("attendance.mark", "mark attendance"),
    OFFERINGS_MANAGE("offerings.manage", "manage plans, services and products"),
    MEMBERSHIPS_MANAGE("memberships.manage", "start, renew or cancel memberships"),
    BILLING_VIEW("billing.view", "see invoices and payments"),
    BILLING_COLLECT("billing.collect", "create invoices and record payments"),
    BILLING_DISCOUNT("billing.discount", "give discounts"),
    BILLING_VOID("billing.void", "void invoices and payments or record refunds"),
    EXPENSES_VIEW("expenses.view", "see expenses"),
    EXPENSES_RECORD("expenses.record", "record expenses"),
    DAYBOOK_CLOSE("daybook.close", "close the day"),
    TEAM_VIEW("team.view", "see the team"),
    TEAM_MANAGE("team.manage", "manage the team and roles"),
    PAYROLL_MANAGE("payroll.manage", "manage payroll"),
    TASKS_MANAGE("tasks.manage", "manage everyone's tasks"),
    REQUESTS_APPROVE("requests.approve", "approve requests"),
    REPORTS_VIEW("reports.view", "see reports"),
    ACTIVITY_VIEW("activity.view", "see the activity log"),
    SETTINGS_MANAGE("settings.manage", "change business settings");

    private static final Map<String, Permission> BY_KEY =
            Arrays.stream(values()).collect(Collectors.toMap(Permission::key, Function.identity()));

    private final String key;
    private final String action;

    Permission(String key, String action) {
        this.key = key;
        this.action = action;
    }

    public String key() {
        return key;
    }

    /** What the permission allows, phrased to complete "You don't have permission to …". */
    public String action() {
        return action;
    }

    public static Optional<Permission> fromKey(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    /** Parses stored keys, ignoring keys that are no longer in the catalogue. */
    public static Set<Permission> fromKeys(String[] keys) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        if (keys != null) {
            for (String key : keys) {
                fromKey(key).ifPresent(permissions::add);
            }
        }
        return permissions;
    }

    public static String[] toKeys(Set<Permission> permissions) {
        return permissions.stream().sorted().map(Permission::key).toArray(String[]::new);
    }
}
