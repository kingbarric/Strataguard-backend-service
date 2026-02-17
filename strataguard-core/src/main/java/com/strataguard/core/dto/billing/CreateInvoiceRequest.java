package com.strataguard.core.dto.billing;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateInvoiceRequest {

    @NotNull(message = "Levy type ID is required")
    private UUID levyTypeId;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String notes;
}
