package com.strataguard.core.dto.security;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StartPatrolRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    private String notes;
}
