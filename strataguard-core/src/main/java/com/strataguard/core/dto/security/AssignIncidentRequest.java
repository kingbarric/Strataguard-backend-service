package com.strataguard.core.dto.security;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AssignIncidentRequest {

    @NotNull(message = "Assigned staff ID is required")
    private UUID assignedTo;
}
