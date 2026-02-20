package com.strataguard.core.dto.security;

import com.strataguard.core.enums.EmergencyAlertType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TriggerEmergencyRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    private UUID unitId;

    @NotNull(message = "Alert type is required")
    private EmergencyAlertType alertType;

    private String description;

    private Double latitude;

    private Double longitude;
}
