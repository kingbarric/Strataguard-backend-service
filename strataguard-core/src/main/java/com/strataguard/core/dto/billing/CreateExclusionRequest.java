package com.strataguard.core.dto.billing;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExclusionRequest {

    @NotNull(message = "Tenancy ID is required")
    private UUID tenancyId;

    private String reason;
}
