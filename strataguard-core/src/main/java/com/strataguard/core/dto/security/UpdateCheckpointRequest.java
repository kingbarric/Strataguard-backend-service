package com.strataguard.core.dto.security;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UpdateCheckpointRequest {

    private UUID estateId;

    private String name;

    private String description;

    private String qrCode;

    private Double latitude;

    private Double longitude;

    private Integer sortOrder;
}
