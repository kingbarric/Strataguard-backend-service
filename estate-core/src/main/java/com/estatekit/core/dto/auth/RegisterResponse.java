package com.estatekit.core.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    @JsonProperty("user_id")
    private String userId;

    private String email;

    private String role;

    @JsonProperty("tenant_id")
    private String tenantId;
}
