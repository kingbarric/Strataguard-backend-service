package com.estatekit.core.dto.unit;

import com.estatekit.core.enums.UnitStatus;
import com.estatekit.core.enums.UnitType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UnitResponse {

    private UUID id;
    private UUID estateId;
    private String unitNumber;
    private String blockOrZone;
    private UnitType unitType;
    private Integer floor;
    private UnitStatus status;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double squareMeters;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
