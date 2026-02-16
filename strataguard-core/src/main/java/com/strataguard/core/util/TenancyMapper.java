package com.strataguard.core.util;

import com.strataguard.core.dto.tenancy.CreateTenancyRequest;
import com.strataguard.core.dto.tenancy.TenancyResponse;
import com.strataguard.core.dto.tenancy.UpdateTenancyRequest;
import com.strataguard.core.entity.Tenancy;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TenancyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.TenancyStatus.ACTIVE)")
    @Mapping(target = "active", expression = "java(true)")
    Tenancy toEntity(CreateTenancyRequest request);

    TenancyResponse toResponse(Tenancy tenancy);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "residentId", ignore = true)
    @Mapping(target = "unitId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateTenancyRequest request, @MappingTarget Tenancy tenancy);
}
