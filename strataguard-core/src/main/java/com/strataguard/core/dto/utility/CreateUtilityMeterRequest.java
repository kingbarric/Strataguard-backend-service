package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.MeterType;
import com.strataguard.core.enums.UtilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CreateUtilityMeterRequest {
    @NotBlank(message = "Meter number is required")
    private String meterNumber;
    private UUID unitId;
    @NotNull(message = "Estate ID is required")
    private UUID estateId;
    @NotNull(message = "Utility type is required")
    private UtilityType utilityType;
    @NotNull(message = "Meter type is required")
    private MeterType meterType;
    private BigDecimal ratePerUnit;
    private String unitOfMeasure;
    private Double consumptionAlertThreshold;
}
