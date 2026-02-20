package com.strataguard.core.dto.governance;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ArtisanRatingResponse {
    private UUID id;
    private UUID artisanId;
    private UUID residentId;
    private UUID maintenanceRequestId;
    private int rating;
    private String review;
    private Instant createdAt;
}
