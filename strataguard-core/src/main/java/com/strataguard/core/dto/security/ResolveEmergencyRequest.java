package com.strataguard.core.dto.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolveEmergencyRequest {

    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;
}
