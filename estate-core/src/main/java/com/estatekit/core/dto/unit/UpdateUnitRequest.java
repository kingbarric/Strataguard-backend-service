package com.estatekit.core.dto.unit;

import com.estatekit.core.enums.UnitStatus;
import com.estatekit.core.enums.UnitType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUnitRequest {

    private String unitNumber;

    private String blockOrZone;

    private UnitType unitType;

    private Integer floor;

    private UnitStatus status;

    private Integer bedrooms;

    private Integer bathrooms;

    private Double squareMeters;

    private String description;

    private Boolean active;
}
