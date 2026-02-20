package com.strataguard.core.dto.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CreateViolationRequest {
    @NotNull
    private UUID estateId;

    @NotNull
    private UUID unitId;

    private UUID residentId;

    @NotBlank
    private String ruleViolated;

    @NotBlank
    private String description;

    private BigDecimal fineAmount;
    private String evidenceUrl;
}
