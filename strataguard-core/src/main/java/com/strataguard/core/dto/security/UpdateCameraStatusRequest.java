package com.strataguard.core.dto.security;

import com.strataguard.core.enums.CameraStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateCameraStatusRequest {

    @NotNull(message = "New status is required")
    private CameraStatus newStatus;

    private String reason;
}
