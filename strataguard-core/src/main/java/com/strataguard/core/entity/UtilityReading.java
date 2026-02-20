package com.strataguard.core.entity;

import com.strataguard.core.enums.UtilityReadingStatus;
import com.strataguard.core.enums.UtilityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "utility_readings", indexes = {
        @Index(name = "idx_utility_readings_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_utility_readings_meter_id", columnList = "meter_id"),
        @Index(name = "idx_utility_readings_unit_id", columnList = "unit_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UtilityReading extends BaseEntity {

    @Column(name = "meter_id", nullable = false)
    private UUID meterId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false)
    private UtilityType utilityType;

    @Column(name = "previous_reading", nullable = false)
    private Double previousReading;

    @Column(name = "current_reading", nullable = false)
    private Double currentReading;

    @Column(nullable = false)
    private Double consumption;

    @Column(name = "rate_per_unit", precision = 15, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(precision = 15, scale = 2)
    private BigDecimal cost;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UtilityReadingStatus status = UtilityReadingStatus.PENDING;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
