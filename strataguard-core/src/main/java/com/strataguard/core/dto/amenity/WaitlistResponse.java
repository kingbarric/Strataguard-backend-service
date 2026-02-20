package com.strataguard.core.dto.amenity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WaitlistResponse {
    private UUID id;
    private UUID amenityId;
    private UUID residentId;
    private Instant desiredStartTime;
    private Instant desiredEndTime;
    private boolean notified;
    private Instant notifiedAt;
    private Instant createdAt;
}
