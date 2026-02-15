package com.estatekit.core.dto.tenancy;

import com.estatekit.core.enums.TenancyType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateTenancyRequest {

    private TenancyType tenancyType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String leaseReference;
}
