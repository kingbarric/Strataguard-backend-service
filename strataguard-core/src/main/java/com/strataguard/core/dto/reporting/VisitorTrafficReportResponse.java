package com.strataguard.core.dto.reporting;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorTrafficReportResponse {
    private long totalVisitors;
    private long checkedIn;
    private long checkedOut;
    private long pending;
    private long expired;
    private long denied;
    private long totalGateSessions;
    private long openGateSessions;
    private long closedGateSessions;
}
