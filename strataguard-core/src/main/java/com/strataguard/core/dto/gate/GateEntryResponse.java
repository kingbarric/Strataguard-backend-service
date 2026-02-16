package com.strataguard.core.dto.gate;

import com.strataguard.core.enums.GateSessionStatus;
import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GateEntryResponse {

    private UUID sessionId;
    private GateSessionStatus status;
    private Instant entryTime;

    // Vehicle details
    private UUID vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private String color;
    private VehicleType vehicleType;
    private VehicleStatus vehicleStatus;

    // Resident details
    private UUID residentId;
    private String residentFirstName;
    private String residentLastName;
    private String residentPhone;
}
