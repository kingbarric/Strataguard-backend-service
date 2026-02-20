package com.strataguard.core.dto.security;

import com.strataguard.core.enums.CameraType;
import com.strataguard.core.enums.CameraZone;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UpdateCameraRequest {

    private UUID estateId;

    private String cameraName;

    private String cameraCode;

    private CameraType cameraType;

    private CameraZone zone;

    private String location;

    private String streamUrl;

    private String ipAddress;

    private Double latitude;

    private Double longitude;

    private LocalDate installDate;

    private String notes;
}
