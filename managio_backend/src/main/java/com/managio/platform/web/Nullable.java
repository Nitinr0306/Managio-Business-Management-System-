package com.managio.platform.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a response field that can be {@code null}. The OpenAPI document publishes it as nullable, so the
 * generated TypeScript types make the frontend handle the missing value. Unmarked response fields are
 * always present and never null.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
public @interface Nullable {
}
