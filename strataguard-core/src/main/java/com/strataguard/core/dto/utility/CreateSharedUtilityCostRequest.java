package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.CostSplitMethod;
import com.strataguard.core.enums.UtilityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateSharedUtilityCostRequest {
    @NotNull(message = "Estate ID is required")
    private UUID estateId;
    @NotNull(message = "Utility type is required")
    private UtilityType utilityType;
    @NotNull(message = "Total cost is required")
    @Positive(message = "Total cost must be positive")
    private BigDecimal totalCost;
    @NotNull(message = "Split method is required")
    private CostSplitMethod splitMethod;
    @NotNull(message = "Billing period start is required")
    private LocalDate billingPeriodStart;
    @NotNull(message = "Billing period end is required")
    private LocalDate billingPeriodEnd;
    private String description;
}
