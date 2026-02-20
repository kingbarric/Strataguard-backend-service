package com.strataguard.core.dto.governance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RateArtisanRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String review;

    private UUID maintenanceRequestId;
}
