package com.estatekit.core.entity;

import com.estatekit.core.enums.VehicleStatus;
import com.estatekit.core.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicles_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_vehicles_resident_id", columnList = "resident_id"),
        @Index(name = "idx_vehicles_plate_number", columnList = "plate_number")
})
@Getter
@Setter
@NoArgsConstructor
public class Vehicle extends BaseEntity {

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "plate_number", nullable = false)
    private String plateNumber;

    @Column(name = "make")
    private String make;

    @Column(name = "model")
    private String model;

    @Column(name = "color")
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "sticker_number")
    private String stickerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false)
    private boolean active = true;
}
