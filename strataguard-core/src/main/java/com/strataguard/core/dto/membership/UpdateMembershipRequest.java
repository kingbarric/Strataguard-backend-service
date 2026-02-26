package com.strataguard.core.dto.membership;

import lombok.*;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMembershipRequest {
    private String role;
    private String status;
    private Set<String> customPermissionsGranted;
    private Set<String> customPermissionsRevoked;
}
