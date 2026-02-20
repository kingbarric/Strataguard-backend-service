package com.strataguard.core.dto.reporting;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SecurityReportResponse {
    private long totalIncidents;
    private long openIncidents;
    private long resolvedIncidents;
    private Map<String, Long> incidentsByCategory;
    private Map<String, Long> incidentsBySeverity;
    private long totalEmergencyAlerts;
    private long activeAlerts;
    private long totalPatrolSessions;
    private long completedPatrols;
    private long camerasOnline;
    private long camerasOffline;
}
