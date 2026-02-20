package com.strataguard.core.dto.security;

import com.strataguard.core.enums.IncidentCategory;
import com.strataguard.core.enums.IncidentSeverity;
import com.strataguard.core.enums.IncidentStatus;
import com.strataguard.core.enums.ReporterType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IncidentResponse {

    private UUID id;
    private UUID estateId;
    private String incidentNumber;
    private UUID reportedBy;
    private ReporterType reporterType;
    private String title;
    private String description;
    private IncidentCategory category;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String location;
    private Double latitude;
    private Double longitude;
    private List<String> photoUrls;
    private List<String> witnesses;
    private UUID linkedAlertId;
    private UUID assignedTo;
    private Instant assignedAt;
    private Instant resolvedAt;
    private UUID resolvedBy;
    private String resolutionNotes;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
