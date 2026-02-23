package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "estate_charge_exclusions", indexes = {
        @Index(name = "idx_estate_charge_exclusions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_estate_charge_exclusions_charge_id", columnList = "estate_charge_id"),
        @Index(name = "idx_estate_charge_exclusions_tenancy_id", columnList = "tenancy_id")
})
@Getter
@Setter
@NoArgsConstructor
public class EstateChargeExclusion extends BaseEntity {

    @Column(name = "estate_charge_id", nullable = false)
    private UUID estateChargeId;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
