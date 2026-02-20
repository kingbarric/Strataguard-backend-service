package com.strataguard.core.dto.maintenance;

import com.strataguard.core.enums.MaintenanceCategory;
import com.strataguard.core.enums.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateMaintenanceRequest {
    private UUID unitId;
    @NotNull(message = "Estate ID is required")
    private UUID estateId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
    @NotNull(message = "Category is required")
    private MaintenanceCategory category;
    @NotNull(message = "Priority is required")
    private MaintenancePriority priority;
    private String photoUrls;
}
