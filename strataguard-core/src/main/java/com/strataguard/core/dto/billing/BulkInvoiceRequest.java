package com.strataguard.core.dto.billing;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BulkInvoiceRequest {

    @NotNull(message = "Levy type ID is required")
    private UUID levyTypeId;

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private LocalDate billingPeriodStart;

    private LocalDate billingPeriodEnd;
}
