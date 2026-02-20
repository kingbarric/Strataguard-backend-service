package com.strataguard.core.dto.security;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PatrolScanResponse {

    private UUID id;
    private UUID sessionId;
    private UUID checkpointId;
    private Instant scannedAt;
    private Double latitude;
    private Double longitude;
    private String notes;
    private String photoUrl;
    private Instant createdAt;
}
