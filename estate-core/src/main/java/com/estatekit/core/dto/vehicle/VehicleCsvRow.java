package com.estatekit.core.dto.vehicle;

import com.estatekit.core.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VehicleCsvRow {

    private UUID residentId;
    private String plateNumber;
    private String make;
    private String model;
    private VehicleType vehicleType;
    private String color;
    private String stickerNumber;
}
