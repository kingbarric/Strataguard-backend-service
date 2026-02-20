package com.strataguard.core.util;

import com.strataguard.core.dto.security.IncidentResponse;
import com.strataguard.core.dto.security.ReportIncidentRequest;
import com.strataguard.core.dto.security.UpdateIncidentRequest;
import com.strataguard.core.entity.SecurityIncident;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SecurityIncidentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "incidentNumber", ignore = true)
    @Mapping(target = "reportedBy", ignore = true)
    @Mapping(target = "reporterType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "resolutionNotes", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    SecurityIncident toEntity(ReportIncidentRequest request);

    IncidentResponse toResponse(SecurityIncident incident);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "estateId", ignore = true)
    @Mapping(target = "incidentNumber", ignore = true)
    @Mapping(target = "reportedBy", ignore = true)
    @Mapping(target = "reporterType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "resolutionNotes", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "linkedAlertId", ignore = true)
    void updateEntity(UpdateIncidentRequest request, @MappingTarget SecurityIncident incident);
}
