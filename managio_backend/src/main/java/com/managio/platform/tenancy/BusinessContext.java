package com.managio.platform.tenancy;

import com.managio.platform.error.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

/**
 * The caller's standing in the business named in the request path, resolved once per request by
 * {@link BusinessAccessInterceptor}. Controllers receive it as a method parameter.
 *
 * @param businessId business from the path
 * @param userId     the signed-in user
 * @param memberId   the user's {@code team_members.id} in this business
 * @param owner      whether the user holds the Owner role
 * @param zone       the business time zone, used for "today" and business dates
 */
public record BusinessContext(long businessId, long userId, long memberId, boolean owner,
                              Set<Permission> permissions, Set<Module> modules, ZoneId zone) {

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }

    public void require(Permission permission) {
        if (!can(permission)) {
            throw ApiException.forbidden("You don't have permission to " + permission.action() + ".");
        }
    }

    public boolean has(Module module) {
        return modules.contains(module);
    }

    /** Today's date in the business time zone. */
    public LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(zone));
    }
}
