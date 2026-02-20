package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.CostSplitMethod;
import com.strataguard.core.enums.UtilityType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SharedUtilityCostResponse {
    private UUID id;
    private UUID estateId;
    private UtilityType utilityType;
    private BigDecimal totalCost;
    private CostSplitMethod splitMethod;
    private Integer totalUnitsParticipating;
    private BigDecimal costPerUnit;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String description;
    private boolean invoicesGenerated;
    private Instant createdAt;
}
