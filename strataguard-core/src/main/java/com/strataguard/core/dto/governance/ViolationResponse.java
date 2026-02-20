package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.ViolationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ViolationResponse {
    private UUID id;
    private UUID estateId;
    private UUID unitId;
    private UUID residentId;
    private String ruleViolated;
    private String description;
    private BigDecimal fineAmount;
    private ViolationStatus status;
    private String reportedBy;
    private String reportedByName;
    private String evidenceUrl;
    private String resolutionNotes;
    private Instant resolvedAt;
    private String appealReason;
    private Instant appealedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
