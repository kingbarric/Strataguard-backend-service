package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private UUID levyTypeId;
    private String levyTypeName;
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
