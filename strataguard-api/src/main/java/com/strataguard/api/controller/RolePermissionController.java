package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.permission.ModifyRolePermissionsRequest;
import com.strataguard.core.dto.permission.RolePermissionsResponse;
import com.strataguard.core.dto.permission.UpdateRolePermissionsRequest;
import com.strataguard.core.enums.Permission;
import com.strataguard.core.enums.UserRole;
import com.strataguard.service.permission.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Role Permissions", description = "SUPER_ADMIN management of role default permissions")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'membership.update')")
    @Operation(summary = "List all roles with their current default permissions")
    public ResponseEntity<ApiResponse<List<RolePermissionsResponse>>> getAllRolesPermissions() {
        List<RolePermissionsResponse> response = rolePermissionService.getAllRolesPermissions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{role}/permissions")
    @PreAuthorize("hasPermission(null, 'membership.update')")
    @Operation(summary = "Get default permissions for a specific role")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> getRolePermissions(
            @PathVariable String role) {
        UserRole userRole = parseRole(role);
        RolePermissionsResponse response = rolePermissionService.getRolePermissions(userRole);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{role}/permissions")
    @PreAuthorize("hasPermission(null, 'membership.update')")
    @Operation(summary = "Replace all default permissions for a role")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> setRolePermissions(
            @PathVariable String role,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        UserRole userRole = parseRole(role);
        RolePermissionsResponse response = rolePermissionService.setRolePermissions(
                userRole, request.getPermissions());
        return ResponseEntity.ok(ApiResponse.success(response, "Role permissions updated"));
    }

    @PatchMapping("/{role}/permissions")
    @PreAuthorize("hasPermission(null, 'membership.update')")
    @Operation(summary = "Add or remove specific permissions from a role")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> modifyRolePermissions(
            @PathVariable String role,
            @Valid @RequestBody ModifyRolePermissionsRequest request) {
        UserRole userRole = parseRole(role);
        RolePermissionsResponse response = rolePermissionService.modifyRolePermissions(
                userRole, request.getGrant(), request.getRevoke());
        return ResponseEntity.ok(ApiResponse.success(response, "Role permissions modified"));
    }

    @GetMapping("/permissions/available")
    @PreAuthorize("hasPermission(null, 'membership.update')")
    @Operation(summary = "List all available permissions from the system")
    public ResponseEntity<ApiResponse<Set<String>>> getAvailablePermissions() {
        Set<String> permissions = Arrays.stream(Permission.values())
                .map(Permission::getValue)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role
                    + ". Valid roles: " + Arrays.toString(UserRole.values()));
        }
    }
}
