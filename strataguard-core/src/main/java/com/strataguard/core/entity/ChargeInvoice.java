package com.strataguard.core.entity;

import com.strataguard.core.enums.ChargeType;
import com.strataguard.core.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "charge_invoices", indexes = {
        @Index(name = "idx_charge_invoices_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_charge_invoices_unit_id", columnList = "unit_id"),
        @Index(name = "idx_charge_invoices_resident_id", columnList = "resident_id"),
        @Index(name = "idx_charge_invoices_status", columnList = "status"),
        @Index(name = "idx_charge_invoices_due_date", columnList = "due_date"),
        @Index(name = "idx_charge_invoices_charge_type", columnList = "charge_type")
})
@Getter
@Setter
@NoArgsConstructor
public class ChargeInvoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 50)
    private ChargeType chargeType;

    @Column(name = "charge_id", nullable = false)
    private UUID chargeId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
