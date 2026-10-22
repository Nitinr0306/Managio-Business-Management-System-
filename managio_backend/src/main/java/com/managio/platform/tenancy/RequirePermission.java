package com.managio.platform.tenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the permissions a business-scoped endpoint needs (all of them). Checked by
 * {@link BusinessAccessInterceptor} before the controller runs. Endpoints without it are open to every active
 * team member of the business.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    Permission[] value();
}
