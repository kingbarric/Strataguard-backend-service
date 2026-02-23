package com.strataguard.core.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExclusionResponse {

    private UUID id;
    private UUID estateChargeId;
    private UUID tenancyId;
    private String reason;
    private Instant createdAt;
}
