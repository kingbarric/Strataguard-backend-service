package com.strataguard.api.config;

import com.strataguard.core.config.EstateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Slf4j
public class StrataguardPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        String permissionStr = permission.toString();

        // SUPER_ADMIN bypass
        boolean isSuperAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) {
            return true;
        }

        // Check from EstateContext (populated by EstateContextFilter)
        return EstateContext.hasPermission(permissionStr);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                  String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
}
