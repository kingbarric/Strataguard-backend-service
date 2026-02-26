package com.strataguard.core.security;

import java.lang.annotation.*;

/**
 * Documents the required permission for an endpoint.
 * Used in conjunction with @PreAuthorize("hasPermission(null, 'permission.value')").
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    String value();
}
