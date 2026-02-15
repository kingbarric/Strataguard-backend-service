package com.estatekit.core.dto.approval;

import com.estatekit.core.enums.ExitApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ExitApprovalResponse {

    private UUID id;
    private UUID sessionId;
    private UUID vehicleId;
    private UUID residentId;
    private String guardId;
    private ExitApprovalStatus status;
    private Instant expiresAt;
    private Instant respondedAt;
    private String note;

    // Vehicle info for resident display
    private String plateNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleColor;

    private Instant createdAt;
    private Instant updatedAt;
}
