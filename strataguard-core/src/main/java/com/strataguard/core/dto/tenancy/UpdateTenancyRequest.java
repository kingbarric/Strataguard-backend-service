package com.strataguard.core.dto.tenancy;

import com.strataguard.core.enums.TenancyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenancyRequest {

    private TenancyType tenancyType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String leaseReference;
}
