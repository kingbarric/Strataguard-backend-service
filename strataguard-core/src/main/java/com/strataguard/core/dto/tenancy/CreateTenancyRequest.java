package com.strataguard.core.dto.tenancy;

import com.strataguard.core.enums.TenancyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
