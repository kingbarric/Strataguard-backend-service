package com.estatekit.core.dto.gate;

import com.estatekit.core.enums.GateSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GateExitResponse {

    private UUID sessionId;
    private GateSessionStatus status;
    private Instant entryTime;
    private Instant exitTime;

    // Vehicle summary
    private UUID vehicleId;
    private String plateNumber;

    // Resident summary
    private UUID residentId;
    private String residentFirstName;
    private String residentLastName;
}
