package com.strataguard.core.dto.security;

import com.strataguard.core.enums.CameraType;
import com.strataguard.core.enums.CameraZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateCameraRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotBlank(message = "Camera name is required")
    private String cameraName;

    @NotBlank(message = "Camera code is required")
    private String cameraCode;

    @NotNull(message = "Camera type is required")
    private CameraType cameraType;

    @NotNull(message = "Camera zone is required")
    private CameraZone zone;

    private String location;

    private String streamUrl;

    private String ipAddress;

    private Double latitude;

    private Double longitude;

    private LocalDate installDate;

    private String notes;
}
