package com.strataguard.core.entity;

import com.strataguard.core.enums.TenancyStatus;
import com.strataguard.core.enums.TenancyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenancies", indexes = {
        @Index(name = "idx_tenancies_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenancies_resident_id", columnList = "resident_id"),
        @Index(name = "idx_tenancies_unit_id", columnList = "unit_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Tenancy extends BaseEntity {

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenancy_type", nullable = false)
    private TenancyType tenancyType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenancyStatus status = TenancyStatus.ACTIVE;

    @Column(name = "lease_reference")
    private String leaseReference;

    @Column(nullable = false)
    private boolean active = true;
}
