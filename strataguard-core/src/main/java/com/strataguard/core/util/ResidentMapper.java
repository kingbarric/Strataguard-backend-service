package com.strataguard.core.util;

import com.strataguard.core.dto.resident.CreateResidentRequest;
import com.strataguard.core.dto.resident.ResidentResponse;
import com.strataguard.core.dto.resident.UpdateResidentRequest;
import com.strataguard.core.entity.Resident;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ResidentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "profilePhotoUrl", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.ResidentStatus.PENDING_VERIFICATION)")
    @Mapping(target = "active", expression = "java(true)")
    Resident toEntity(CreateResidentRequest request);

    ResidentResponse toResponse(Resident resident);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void updateEntity(UpdateResidentRequest request, @MappingTarget Resident resident);
}
