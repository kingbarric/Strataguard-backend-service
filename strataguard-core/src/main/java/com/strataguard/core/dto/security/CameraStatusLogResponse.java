package com.strataguard.core.dto.security;

import com.strataguard.core.enums.CameraStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CameraStatusLogResponse {

    private UUID id;
    private UUID cameraId;
    private CameraStatus previousStatus;
    private CameraStatus newStatus;
    private Instant changedAt;
    private String reason;
    private Instant createdAt;
}
