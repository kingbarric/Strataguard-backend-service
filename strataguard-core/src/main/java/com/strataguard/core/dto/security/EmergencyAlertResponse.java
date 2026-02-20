package com.strataguard.core.dto.security;

import com.strataguard.core.enums.EmergencyAlertStatus;
import com.strataguard.core.enums.EmergencyAlertType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class EmergencyAlertResponse {

    private UUID id;
    private UUID residentId;
    private UUID estateId;
    private UUID unitId;
    private EmergencyAlertType alertType;
    private EmergencyAlertStatus status;
    private String description;
    private Double latitude;
    private Double longitude;
    private UUID acknowledgedBy;
    private Instant acknowledgedAt;
    private UUID respondedBy;
    private Instant respondedAt;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private String resolutionNotes;
    private Long responseTimeSeconds;
    private Instant createdAt;
    private Instant updatedAt;
}
