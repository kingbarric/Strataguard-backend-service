package com.strataguard.core.dto.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatrolScanRequest {

    @NotBlank(message = "QR code is required")
    private String qrCode;

    private Double latitude;

    private Double longitude;

    private String notes;

    private String photoUrl;
}
