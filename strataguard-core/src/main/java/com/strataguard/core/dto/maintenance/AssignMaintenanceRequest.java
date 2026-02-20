package com.strataguard.core.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignMaintenanceRequest {
    @NotBlank(message = "Assignee name is required")
    private String assignedTo;
    private String assignedToPhone;
}
