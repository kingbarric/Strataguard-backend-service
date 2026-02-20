package com.strataguard.core.dto.reporting;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OccupancyReportResponse {
    private long totalUnits;
    private long occupiedUnits;
    private long vacantUnits;
    private long underMaintenanceUnits;
    private long reservedUnits;
    private double occupancyRate;
    private Map<String, Long> unitsByStatus;
    private List<EstateOccupancy> byEstate;

    @Data
    @Builder
    public static class EstateOccupancy {
        private String estateId;
        private String estateName;
        private long totalUnits;
        private long occupiedUnits;
        private double occupancyRate;
    }
}
