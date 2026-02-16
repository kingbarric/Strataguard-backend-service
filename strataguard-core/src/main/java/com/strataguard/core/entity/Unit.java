package com.strataguard.core.entity;

import com.strataguard.core.enums.UnitStatus;
import com.strataguard.core.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "units", indexes = {
        @Index(name = "idx_units_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_units_estate_id", columnList = "estate_id"),
        @Index(name = "idx_units_unit_number", columnList = "unit_number, estate_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Unit extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @Column(name = "block_or_zone")
    private String blockOrZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status = UnitStatus.VACANT;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "square_meters")
    private Double squareMeters;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
