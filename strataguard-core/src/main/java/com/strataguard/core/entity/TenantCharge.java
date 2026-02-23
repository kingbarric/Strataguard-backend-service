package com.strataguard.core.entity;

import com.strataguard.core.enums.LevyFrequency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tenant_charges", indexes = {
        @Index(name = "idx_tenant_charges_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenant_charges_tenancy_id", columnList = "tenancy_id"),
        @Index(name = "idx_tenant_charges_estate_id", columnList = "estate_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TenantCharge extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LevyFrequency frequency;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(length = 100)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reminder_days_before", columnDefinition = "jsonb")
    private List<Integer> reminderDaysBefore;

    @Column(nullable = false)
    private boolean active = true;
}
