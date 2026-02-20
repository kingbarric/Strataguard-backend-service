package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "patrol_checkpoints", indexes = {
        @Index(name = "idx_patrol_checkpoints_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_patrol_checkpoints_estate_id", columnList = "estate_id")
})
@Getter
@Setter
public class PatrolCheckpoint extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "qr_code", nullable = false)
    private String qrCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
