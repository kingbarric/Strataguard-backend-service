package com.strataguard.core.dto.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcknowledgeEmergencyRequest {

    private String notes;
}
