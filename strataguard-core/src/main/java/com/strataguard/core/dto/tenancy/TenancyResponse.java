package com.strataguard.core.dto.tenancy;

import com.strataguard.core.enums.TenancyStatus;
import com.strataguard.core.enums.TenancyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancyResponse {

    private UUID id;
    private UUID residentId;
    private UUID unitId;
    private TenancyType tenancyType;
    private LocalDate startDate;
    private LocalDate endDate;
    private TenancyStatus status;
    private String leaseReference;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
