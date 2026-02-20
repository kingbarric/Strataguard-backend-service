package com.strataguard.core.util;

import com.strataguard.core.dto.maintenance.MaintenanceResponse;
import com.strataguard.core.entity.MaintenanceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    @Mapping(target = "unitNumber", ignore = true)
    @Mapping(target = "residentName", ignore = true)
    MaintenanceResponse toResponse(MaintenanceRequest request);
}
