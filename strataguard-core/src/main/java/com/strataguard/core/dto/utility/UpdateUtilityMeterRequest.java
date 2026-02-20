package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.MeterType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpdateUtilityMeterRequest {
    private MeterType meterType;
    private BigDecimal ratePerUnit;
    private String unitOfMeasure;
    private Double consumptionAlertThreshold;
}
