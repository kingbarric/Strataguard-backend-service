package com.strataguard.core.dto.membership;

import lombok.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstateMembershipResponse {
    private UUID id;
    private String userId;
    private UUID estateId;
    private String estateName;
    private String role;
    private String status;
    private String displayName;
    private Set<String> effectivePermissions;
    private Instant createdAt;
}
