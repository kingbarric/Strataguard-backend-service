package com.strataguard.core.dto.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SecurityDashboardResponse {

    private long onDutyStaffCount;
    private long openIncidents;
    private long criticalIncidents;
    private long highIncidents;
    private double patrolCompletionRate;
    private double avgEmergencyResponseSeconds;
    private long activeEmergencies;
    private long totalCameras;
    private long onlineCameras;
    private long offlineCameras;
    private List<IncidentResponse> recentIncidents;
}
