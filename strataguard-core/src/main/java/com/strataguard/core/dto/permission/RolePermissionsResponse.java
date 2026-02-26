package com.strataguard.core.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsResponse {

    private String role;

    private Set<String> permissions;

    private Set<String> availablePermissions;
}
