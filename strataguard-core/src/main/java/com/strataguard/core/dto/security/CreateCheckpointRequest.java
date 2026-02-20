package com.strataguard.core.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateCheckpointRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotBlank(message = "Checkpoint name is required")
    private String name;

    private String description;

    @NotBlank(message = "QR code is required")
    private String qrCode;

    private Double latitude;

    private Double longitude;

    private Integer sortOrder;
}
