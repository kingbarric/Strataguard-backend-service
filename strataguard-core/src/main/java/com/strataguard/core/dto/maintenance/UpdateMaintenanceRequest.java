package com.strataguard.core.dto.maintenance;

import com.strataguard.core.enums.MaintenanceCategory;
import com.strataguard.core.enums.MaintenancePriority;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateMaintenanceRequest {
    private String title;
    private String description;
    private MaintenanceCategory category;
    private MaintenancePriority priority;
    private String photoUrls;
}
