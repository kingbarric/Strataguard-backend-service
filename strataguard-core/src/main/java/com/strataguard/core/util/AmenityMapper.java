package com.strataguard.core.util;

import com.strataguard.core.dto.amenity.AmenityResponse;
import com.strataguard.core.dto.amenity.CreateAmenityRequest;
import com.strataguard.core.dto.amenity.UpdateAmenityRequest;
import com.strataguard.core.entity.Amenity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    Amenity toEntity(CreateAmenityRequest request);

    AmenityResponse toResponse(Amenity amenity);

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
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateAmenityRequest request, @MappingTarget Amenity amenity);
}
