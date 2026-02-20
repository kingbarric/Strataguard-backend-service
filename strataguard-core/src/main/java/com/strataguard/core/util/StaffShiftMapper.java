package com.strataguard.core.util;

import com.strataguard.core.dto.security.CreateStaffShiftRequest;
import com.strataguard.core.dto.security.StaffShiftResponse;
import com.strataguard.core.entity.StaffShift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffShiftMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "staffId", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    StaffShift toEntity(CreateStaffShiftRequest request);

    StaffShiftResponse toResponse(StaffShift staffShift);
}
