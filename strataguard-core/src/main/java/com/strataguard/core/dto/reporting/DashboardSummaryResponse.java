package com.strataguard.core.dto.reporting;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryResponse {
    // Property Management
    private long totalEstates;
    private long totalUnits;
    private double occupancyRate;
    private long totalResidents;

    // Financial
    private BigDecimal totalRevenue;
    private BigDecimal outstandingAmount;
    private double collectionRate;

    // Security
    private long openIncidents;
    private long activeAlerts;

    // Operations
    private long openMaintenanceRequests;
    private long pendingComplaints;
    private long activePolls;

    // Visitors
    private long todayVisitors;
    private long openGateSessions;
}
