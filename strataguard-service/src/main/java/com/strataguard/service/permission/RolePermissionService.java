package com.strataguard.service.permission;

import com.strataguard.core.dto.permission.RolePermissionsResponse;
import com.strataguard.core.entity.RolePermissionDefault;
import com.strataguard.core.enums.Permission;
import com.strataguard.core.enums.UserRole;
import com.strataguard.infrastructure.repository.RolePermissionDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final RolePermissionDefaultRepository rolePermissionDefaultRepository;

    private static final Set<String> ALL_PERMISSION_VALUES = Arrays.stream(Permission.values())
            .map(Permission::getValue)
            .collect(Collectors.toUnmodifiableSet());

    public RolePermissionsResponse getRolePermissions(UserRole role) {
        List<String> permissions = rolePermissionDefaultRepository.findPermissionsByRole(role);

        return RolePermissionsResponse.builder()
                .role(role.name())
                .permissions(new LinkedHashSet<>(permissions))
                .availablePermissions(ALL_PERMISSION_VALUES)
                .build();
    }

    public List<RolePermissionsResponse> getAllRolesPermissions() {
        return Arrays.stream(UserRole.values())
                .filter(r -> r != UserRole.RESIDENT) // skip deprecated
                .map(role -> {
                    List<String> permissions = rolePermissionDefaultRepository.findPermissionsByRole(role);
                    return RolePermissionsResponse.builder()
                            .role(role.name())
                            .permissions(new LinkedHashSet<>(permissions))
                            .build();
                })
                .toList();
    }

    @Transactional
    @CacheEvict(value = "rolePermissions", allEntries = true)
    public RolePermissionsResponse setRolePermissions(UserRole role, Set<String> permissions) {
        validateNotSuperAdmin(role);
        validatePermissionNames(permissions);

        rolePermissionDefaultRepository.deleteAllByRole(role);

        List<RolePermissionDefault> entities = permissions.stream()
                .map(perm -> {
                    RolePermissionDefault rpd = new RolePermissionDefault();
                    rpd.setRole(role);
                    rpd.setPermission(perm);
                    return rpd;
                })
                .toList();

        rolePermissionDefaultRepository.saveAll(entities);
        log.info("Replaced permissions for role {}: {} permissions set", role, permissions.size());

        return getRolePermissions(role);
    }

    @Transactional
    @CacheEvict(value = "rolePermissions", allEntries = true)
    public RolePermissionsResponse modifyRolePermissions(UserRole role, Set<String> grant, Set<String> revoke) {
        validateNotSuperAdmin(role);

        if (grant != null && !grant.isEmpty()) {
            validatePermissionNames(grant);
            for (String perm : grant) {
                if (!rolePermissionDefaultRepository.existsByRoleAndPermission(role, perm)) {
                    RolePermissionDefault rpd = new RolePermissionDefault();
                    rpd.setRole(role);
                    rpd.setPermission(perm);
                    rolePermissionDefaultRepository.save(rpd);
                }
            }
            log.info("Granted {} permissions to role {}", grant.size(), role);
        }

        if (revoke != null && !revoke.isEmpty()) {
            validatePermissionNames(revoke);
            for (String perm : revoke) {
                rolePermissionDefaultRepository.deleteByRoleAndPermission(role, perm);
            }
            log.info("Revoked {} permissions from role {}", revoke.size(), role);
        }

        return getRolePermissions(role);
    }

    private void validateNotSuperAdmin(UserRole role) {
        if (role == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot modify SUPER_ADMIN role permissions. "
                    + "SUPER_ADMIN always has all permissions.");
        }
    }

    private void validatePermissionNames(Set<String> permissions) {
        Set<String> invalid = permissions.stream()
                .filter(p -> !ALL_PERMISSION_VALUES.contains(p))
                .collect(Collectors.toSet());

        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Invalid permission names: " + invalid);
        }
    }
}
