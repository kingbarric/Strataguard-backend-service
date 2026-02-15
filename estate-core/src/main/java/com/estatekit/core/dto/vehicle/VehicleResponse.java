package com.estatekit.core.dto.vehicle;

import com.estatekit.core.enums.VehicleStatus;
import com.estatekit.core.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VehicleResponse {

    private UUID id;
    private UUID residentId;
    private String plateNumber;
    private String make;
    private String model;
    private String color;
    private VehicleType vehicleType;
    private String qrStickerCode;
    private String stickerNumber;
    private VehicleStatus status;
    private String photoUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
