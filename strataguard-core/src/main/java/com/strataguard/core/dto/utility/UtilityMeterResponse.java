package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.MeterType;
import com.strataguard.core.enums.UtilityType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UtilityMeterResponse {
    private UUID id;
    private String meterNumber;
    private UUID unitId;
    private String unitNumber;
    private UUID estateId;
    private UtilityType utilityType;
    private MeterType meterType;
    private BigDecimal ratePerUnit;
    private String unitOfMeasure;
    private Double consumptionAlertThreshold;
    private Double lastReadingValue;
    private LocalDate lastReadingDate;
    private boolean active;
    private Instant createdAt;
}
