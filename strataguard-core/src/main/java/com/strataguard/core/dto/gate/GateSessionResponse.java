package com.strataguard.core.dto.gate;

import com.strataguard.core.enums.GateSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GateSessionResponse {

    private UUID id;
    private UUID vehicleId;
    private UUID residentId;
    private String plateNumber;
    private GateSessionStatus status;
    private Instant entryTime;
    private Instant exitTime;
    private String entryGuardId;
    private String exitGuardId;
    private String entryNote;
    private String exitNote;
    private Instant createdAt;
    private Instant updatedAt;
}
