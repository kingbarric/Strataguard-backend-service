package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.ChargeType;
import com.strataguard.core.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private ChargeType chargeType;
    private UUID chargeId;
    private String chargeName;
    private UUID unitId;
    private String unitNumber;
    private UUID residentId;
    private String residentName;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
