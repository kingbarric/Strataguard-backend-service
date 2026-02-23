package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.ChargeType;
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
public class CreateInvoiceRequest {

    @NotNull(message = "Charge ID is required")
    private UUID chargeId;

    @NotNull(message = "Charge type is required")
    private ChargeType chargeType;

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String notes;
}
