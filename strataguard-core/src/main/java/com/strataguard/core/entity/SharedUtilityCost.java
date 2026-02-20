package com.strataguard.core.entity;

import com.strataguard.core.enums.CostSplitMethod;
import com.strataguard.core.enums.UtilityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shared_utility_costs", indexes = {
        @Index(name = "idx_shared_utility_costs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_shared_utility_costs_estate_id", columnList = "estate_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SharedUtilityCost extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false)
    private UtilityType utilityType;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_method", nullable = false)
    private CostSplitMethod splitMethod;

    @Column(name = "total_units_participating")
    private Integer totalUnitsParticipating;

    @Column(name = "cost_per_unit", precision = 15, scale = 2)
    private BigDecimal costPerUnit;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "invoices_generated", nullable = false)
    private boolean invoicesGenerated = false;
}
