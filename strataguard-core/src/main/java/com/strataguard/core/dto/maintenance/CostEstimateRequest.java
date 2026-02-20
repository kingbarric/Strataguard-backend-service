package com.strataguard.core.dto.maintenance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CostEstimateRequest {
    @NotNull(message = "Estimated cost is required")
    @Positive(message = "Estimated cost must be positive")
    private BigDecimal estimatedCost;
    private String notes;
}
