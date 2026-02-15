package com.estatekit.core.dto.estate;

import com.estatekit.core.enums.EstateType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class EstateResponse {

    private UUID id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private EstateType estateType;
    private String description;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;
    private Integer totalUnits;
    private String settings;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
