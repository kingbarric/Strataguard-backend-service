package com.strataguard.core.util;

import com.strataguard.core.dto.vehicle.CreateVehicleRequest;
import com.strataguard.core.dto.vehicle.UpdateVehicleRequest;
import com.strataguard.core.dto.vehicle.VehicleResponse;
import com.strataguard.core.entity.Vehicle;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.VehicleStatus.ACTIVE)")
    @Mapping(target = "qrStickerCode", ignore = true)
    @Mapping(target = "photoUrl", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    Vehicle toEntity(CreateVehicleRequest request);

    VehicleResponse toResponse(Vehicle vehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "residentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "qrStickerCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(UpdateVehicleRequest request, @MappingTarget Vehicle vehicle);
}
