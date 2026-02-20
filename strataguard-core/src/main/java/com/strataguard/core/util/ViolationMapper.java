package com.strataguard.core.util;

import com.strataguard.core.dto.governance.CreateViolationRequest;
import com.strataguard.core.dto.governance.ViolationResponse;
import com.strataguard.core.entity.Violation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ViolationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.ViolationStatus.REPORTED)")
    @Mapping(target = "reportedBy", ignore = true)
    @Mapping(target = "reportedByName", ignore = true)
    @Mapping(target = "resolutionNotes", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "appealReason", ignore = true)
    @Mapping(target = "appealedAt", ignore = true)
    Violation toEntity(CreateViolationRequest request);

    ViolationResponse toResponse(Violation violation);
}
