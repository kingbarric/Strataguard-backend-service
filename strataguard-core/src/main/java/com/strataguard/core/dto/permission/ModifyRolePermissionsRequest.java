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
public class ModifyRolePermissionsRequest {

    private Set<String> grant;

    private Set<String> revoke;
}
