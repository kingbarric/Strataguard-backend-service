package com.strataguard.core.dto.maintenance;

import com.strataguard.core.enums.MaintenanceCategory;
import com.strataguard.core.enums.MaintenancePriority;
import com.strataguard.core.enums.MaintenanceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MaintenanceResponse {
    private UUID id;
    private String requestNumber;
    private UUID unitId;
    private String unitNumber;
    private UUID estateId;
    private UUID residentId;
    private String residentName;
    private String title;
    private String description;
    private MaintenanceCategory category;
    private MaintenancePriority priority;
    private MaintenanceStatus status;
    private String assignedTo;
    private String assignedToPhone;
    private Instant assignedAt;
    private String photoUrls;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String costApprovedBy;
    private Instant costApprovedAt;
    private Instant slaDeadline;
    private boolean slaBreached;
    private boolean escalated;
    private Instant escalatedAt;
    private Instant resolvedAt;
    private Instant closedAt;
    private String resolutionNotes;
    private Integer satisfactionRating;
    private String satisfactionComment;
    private Instant createdAt;
    private Instant updatedAt;
}
