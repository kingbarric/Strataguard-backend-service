package com.strataguard.core.dto.vehicle;

import com.strataguard.core.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateVehicleRequest {

    private String plateNumber;

    private String make;

    private String model;

    private String color;

    private VehicleType vehicleType;

    private String stickerNumber;

    private String photoUrl;

    private Boolean active;
}
