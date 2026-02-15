package com.estatekit.core.dto.vehicle;

import com.estatekit.core.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateVehicleRequest {

    @NotNull(message = "Resident ID is required")
    private UUID residentId;

    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    private String make;

    private String model;

    private String color;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String stickerNumber;
}
