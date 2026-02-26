package com.strataguard.core.dto.membership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMembershipRequest {
    @NotBlank
    private String userId;

    @NotNull
    private UUID estateId;

    @NotBlank
    private String role;

    private Set<String> customPermissionsGranted;
    private Set<String> customPermissionsRevoked;
}
