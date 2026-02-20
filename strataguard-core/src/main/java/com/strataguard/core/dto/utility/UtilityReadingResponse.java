package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.UtilityReadingStatus;
import com.strataguard.core.enums.UtilityType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UtilityReadingResponse {
    private UUID id;
    private UUID meterId;
    private String meterNumber;
    private UUID unitId;
    private UtilityType utilityType;
    private Double previousReading;
    private Double currentReading;
    private Double consumption;
    private BigDecimal ratePerUnit;
    private BigDecimal cost;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private LocalDate readingDate;
    private UtilityReadingStatus status;
    private UUID invoiceId;
    private String notes;
    private Instant createdAt;
}
