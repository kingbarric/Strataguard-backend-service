package com.strataguard.core.util;

import com.strataguard.core.dto.billing.CreateLevyTypeRequest;
import com.strataguard.core.dto.billing.LevyTypeResponse;
import com.strataguard.core.dto.billing.UpdateLevyTypeRequest;
import com.strataguard.core.entity.LevyType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LevyTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    LevyType toEntity(CreateLevyTypeRequest request);

    @Mapping(target = "estateName", ignore = true)
    LevyTypeResponse toResponse(LevyType levyType);

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
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateLevyTypeRequest request, @MappingTarget LevyType levyType);
}
