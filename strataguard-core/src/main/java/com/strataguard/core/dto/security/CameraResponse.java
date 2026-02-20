package com.strataguard.core.dto.security;

import com.strataguard.core.enums.CameraStatus;
import com.strataguard.core.enums.CameraType;
import com.strataguard.core.enums.CameraZone;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CameraResponse {

    private UUID id;
    private UUID estateId;
    private String cameraName;
    private String cameraCode;
    private CameraType cameraType;
    private CameraZone zone;
    private String location;
    private String streamUrl;
    private String ipAddress;
    private CameraStatus status;
    private Instant lastOnlineAt;
    private Double latitude;
    private Double longitude;
    private LocalDate installDate;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
