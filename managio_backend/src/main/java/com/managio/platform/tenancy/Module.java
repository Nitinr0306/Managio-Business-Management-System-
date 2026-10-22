package com.managio.platform.tenancy;

import java.util.EnumSet;
import java.util.Set;

/** Optional modules a business can switch on or off (docs/PRODUCT.md#business-types). Core modules are always on. */
public enum Module {
    MEMBERSHIPS,
    CATALOG,
    INVENTORY,
    GROUPS,
    ATTENDANCE,
    PAYROLL;

    /** Parses stored module names, ignoring names that are no longer modules. */
    public static Set<Module> fromNames(String[] names) {
        Set<Module> modules = EnumSet.noneOf(Module.class);
        if (names != null) {
            for (String name : names) {
                for (Module module : values()) {
                    if (module.name().equals(name)) {
                        modules.add(module);
                    }
                }
            }
        }
        return modules;
    }

    public static String[] toNames(Set<Module> modules) {
        return modules.stream().sorted().map(Module::name).toArray(String[]::new);
    }
}
