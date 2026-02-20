package com.strataguard.core.dto.utility;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UtilityDashboardResponse {
    private long totalMeters;
    private long activeMeters;
    private long pendingReadings;
    private long validatedReadings;
    private BigDecimal totalUtilityCosts;
    private long sharedCostsCount;
}
