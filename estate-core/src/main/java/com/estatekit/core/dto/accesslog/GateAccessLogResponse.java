package com.estatekit.core.dto.accesslog;

import com.estatekit.core.enums.GateEventType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GateAccessLogResponse {

    private UUID id;
    private UUID sessionId;
    private UUID vehicleId;
    private UUID residentId;
    private UUID visitorId;
    private GateEventType eventType;
    private String guardId;
    private String details;
    private boolean success;
    private Instant createdAt;
}
