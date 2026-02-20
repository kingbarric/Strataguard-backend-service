package com.strataguard.core.entity;

import com.strataguard.core.enums.MeterType;
import com.strataguard.core.enums.UtilityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "utility_meters", indexes = {
        @Index(name = "idx_utility_meters_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_utility_meters_unit_id", columnList = "unit_id"),
        @Index(name = "idx_utility_meters_estate_id", columnList = "estate_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UtilityMeter extends BaseEntity {

    @Column(name = "meter_number", nullable = false)
    private String meterNumber;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false)
    private UtilityType utilityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false)
    private MeterType meterType;

    @Column(name = "rate_per_unit", precision = 15, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "consumption_alert_threshold")
    private Double consumptionAlertThreshold;

    @Column(name = "last_reading_value")
    private Double lastReadingValue;

    @Column(name = "last_reading_date")
    private LocalDate lastReadingDate;

    @Column(nullable = false)
    private boolean active = true;
}
