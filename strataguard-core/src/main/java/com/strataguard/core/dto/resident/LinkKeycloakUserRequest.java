package com.strataguard.core.dto.resident;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkKeycloakUserRequest {

    @NotBlank(message = "Keycloak user ID is required")
    private String userId;
}
