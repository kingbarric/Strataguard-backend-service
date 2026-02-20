package com.strataguard.core.dto.security;

import com.strataguard.core.enums.IncidentCategory;
import com.strataguard.core.enums.IncidentSeverity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UpdateIncidentRequest {

    private String title;

    private String description;

    private IncidentCategory category;

    private IncidentSeverity severity;

    private String location;

    private Double latitude;

    private Double longitude;

    private List<String> photoUrls;

    private List<String> witnesses;
}
