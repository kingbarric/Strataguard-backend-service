package com.strataguard.core.dto.maintenance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaintenanceDashboardResponse {
    private long totalRequests;
    private long openRequests;
    private long assignedRequests;
    private long inProgressRequests;
    private long resolvedRequests;
    private long slaBreachedRequests;
    private double averageSatisfactionRating;
}
