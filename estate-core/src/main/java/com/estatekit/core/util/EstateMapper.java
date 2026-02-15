package com.estatekit.core.util;

import com.estatekit.core.dto.estate.CreateEstateRequest;
import com.estatekit.core.dto.estate.EstateResponse;
import com.estatekit.core.entity.Estate;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EstateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    Estate toEntity(CreateEstateRequest request);

    EstateResponse toResponse(Estate estate);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    void updateEntity(com.estatekit.core.dto.estate.UpdateEstateRequest request, @MappingTarget Estate estate);
}
