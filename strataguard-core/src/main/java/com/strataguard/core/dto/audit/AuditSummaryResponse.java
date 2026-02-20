package com.strataguard.core.dto.audit;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AuditSummaryResponse {
    private long totalEvents;
    private Map<String, Long> eventsByAction;
    private Map<String, Long> eventsByEntityType;
    private Map<String, Long> topActors;
}
