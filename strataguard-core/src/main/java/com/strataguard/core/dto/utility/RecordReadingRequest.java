package com.strataguard.core.dto.utility;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class RecordReadingRequest {
    @NotNull(message = "Meter ID is required")
    private UUID meterId;
    @NotNull(message = "Current reading is required")
    private Double currentReading;
    @NotNull(message = "Reading date is required")
    private LocalDate readingDate;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String notes;
}
