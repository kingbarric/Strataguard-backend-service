package com.estatekit.core.dto.unit;

import com.estatekit.core.enums.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateUnitRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotBlank(message = "Unit number is required")
    private String unitNumber;

    private String blockOrZone;

    @NotNull(message = "Unit type is required")
    private UnitType unitType;

    private Integer floor;

    private Integer bedrooms;

    private Integer bathrooms;

    private Double squareMeters;

    private String description;
}
