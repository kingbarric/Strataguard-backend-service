package com.strataguard.core.dto.vehicle;

import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
