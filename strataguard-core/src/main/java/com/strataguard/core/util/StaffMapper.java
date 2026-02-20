package com.strataguard.core.util;

import com.strataguard.core.dto.security.CreateStaffRequest;
import com.strataguard.core.dto.security.StaffResponse;
import com.strataguard.core.dto.security.UpdateStaffRequest;
import com.strataguard.core.entity.Staff;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.StaffStatus.ACTIVE)")
    @Mapping(target = "active", expression = "java(true)")
    Staff toEntity(CreateStaffRequest request);

    StaffResponse toResponse(Staff staff);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "estateId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateStaffRequest request, @MappingTarget Staff staff);
}
