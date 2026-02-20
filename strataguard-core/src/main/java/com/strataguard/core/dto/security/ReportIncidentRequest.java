package com.strataguard.core.dto.security;

import com.strataguard.core.enums.IncidentCategory;
import com.strataguard.core.enums.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReportIncidentRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private IncidentCategory category;

    @NotNull(message = "Severity is required")
    private IncidentSeverity severity;

    private String location;

    private Double latitude;

    private Double longitude;

    private List<String> photoUrls;

    private List<String> witnesses;

    private UUID linkedAlertId;
}
