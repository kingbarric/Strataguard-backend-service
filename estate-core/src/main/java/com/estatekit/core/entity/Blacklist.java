package com.estatekit.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "blacklist", indexes = {
        @Index(name = "idx_blacklist_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_blacklist_plate_number", columnList = "plate_number"),
        @Index(name = "idx_blacklist_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
public class Blacklist extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "added_by", nullable = false)
    private String addedBy;
}
