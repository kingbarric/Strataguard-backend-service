package com.strataguard.core.dto.utility;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UtilityStatementResponse {
    private UUID unitId;
    private String unitNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private List<UtilityReadingResponse> readings;
    private BigDecimal totalCost;
}
