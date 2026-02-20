package com.strataguard.core.dto.utility;

import com.strataguard.core.enums.UtilityType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConsumptionTrendResponse {
    private UUID meterId;
    private String meterNumber;
    private UtilityType utilityType;
    private List<MonthlyConsumption> monthlyData;

    @Data
    @Builder
    public static class MonthlyConsumption {
        private String month;
        private Double consumption;
        private BigDecimal cost;
    }
}
