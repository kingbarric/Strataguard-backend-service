package com.strataguard.service.permission;

import com.strataguard.core.entity.EstateMembership;
import com.strataguard.core.enums.UserRole;
import com.strataguard.infrastructure.repository.RolePermissionDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionResolver {

    private final RolePermissionDefaultRepository rolePermissionDefaultRepository;

    /**
     * Resolve effective permissions for a membership.
     * Formula: (default role permissions + custom granted) - custom revoked
     */
    public Set<String> resolvePermissions(EstateMembership membership) {
        Set<String> effective = new HashSet<>(getDefaultPermissions(membership.getRole()));

        if (membership.getCustomPermissionsGranted() != null) {
            effective.addAll(Arrays.asList(membership.getCustomPermissionsGranted()));
        }

        if (membership.getCustomPermissionsRevoked() != null) {
            for (String revoked : membership.getCustomPermissionsRevoked()) {
                effective.remove(revoked);
            }
        }

        return Collections.unmodifiableSet(effective);
    }

    @Cacheable(value = "rolePermissions", key = "#role.name()")
    public List<String> getDefaultPermissions(UserRole role) {
        return rolePermissionDefaultRepository.findPermissionsByRole(role);
    }

    public boolean isSuperAdmin(Collection<String> jwtRoles) {
        return jwtRoles.contains("SUPER_ADMIN") || jwtRoles.contains("ROLE_SUPER_ADMIN");
    }
}
