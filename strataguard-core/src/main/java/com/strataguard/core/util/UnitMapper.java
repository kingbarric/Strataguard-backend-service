package com.strataguard.core.util;

import com.strataguard.core.dto.unit.CreateUnitRequest;
import com.strataguard.core.dto.unit.UnitResponse;
import com.strataguard.core.entity.Unit;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UnitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.UnitStatus.VACANT)")
    @Mapping(target = "active", expression = "java(true)")
    Unit toEntity(CreateUnitRequest request);

    UnitResponse toResponse(Unit unit);

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
    void updateEntity(com.strataguard.core.dto.unit.UpdateUnitRequest request, @MappingTarget Unit unit);
}
