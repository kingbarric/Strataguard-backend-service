package com.strataguard.core.entity;

import com.strataguard.core.enums.LevyFrequency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "levy_types", indexes = {
        @Index(name = "idx_levy_types_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_levy_types_estate_id", columnList = "estate_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LevyType extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LevyFrequency frequency;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active = true;
}
