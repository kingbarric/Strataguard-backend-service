package com.strataguard.core.dto.security;

import com.strataguard.core.enums.PatrolSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PatrolSessionResponse {

    private UUID id;
    private UUID staffId;
    private UUID estateId;
    private Instant startedAt;
    private Instant completedAt;
    private PatrolSessionStatus status;
    private int totalCheckpoints;
    private int scannedCheckpoints;
    private Double completionPercentage;
    private String notes;
    private List<PatrolScanResponse> scans;
    private Instant createdAt;
    private Instant updatedAt;
}
