package com.estatekit.core.dto.exitpass;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ExitPassResponse {

    private UUID vehicleId;
    private String token;
    private Instant expiresAt;
}
