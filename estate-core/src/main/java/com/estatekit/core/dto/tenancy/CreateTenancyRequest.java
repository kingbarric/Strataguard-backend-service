package com.estatekit.core.dto.tenancy;

import com.estatekit.core.enums.TenancyType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateTenancyRequest {

    @NotNull(message = "Resident ID is required")
    private UUID residentId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Tenancy type is required")
    private TenancyType tenancyType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private String leaseReference;
}
