package com.strataguard.core.util;

import com.strataguard.core.dto.governance.ComplaintResponse;
import com.strataguard.core.dto.governance.CreateComplaintRequest;
import com.strataguard.core.entity.Complaint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ComplaintMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "residentId", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.ComplaintStatus.OPEN)")
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "assignedToName", ignore = true)
    @Mapping(target = "responseNotes", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    Complaint toEntity(CreateComplaintRequest request);

    ComplaintResponse toResponse(Complaint complaint);
}
