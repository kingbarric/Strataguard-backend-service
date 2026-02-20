package com.strataguard.core.dto.security;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CheckpointResponse {

    private UUID id;
    private UUID estateId;
    private String name;
    private String description;
    private String qrCode;
    private Double latitude;
    private Double longitude;
    private Integer sortOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
