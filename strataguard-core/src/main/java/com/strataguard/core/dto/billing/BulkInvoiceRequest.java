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
public class BulkInvoiceRequest {

    @NotNull(message = "Charge ID is required")
    private UUID chargeId;

    private ChargeType chargeType;

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private LocalDate billingPeriodStart;

    private LocalDate billingPeriodEnd;
}
