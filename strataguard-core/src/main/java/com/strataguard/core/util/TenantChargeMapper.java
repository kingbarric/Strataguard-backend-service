package com.strataguard.core.util;

import com.strataguard.core.dto.billing.CreateTenantChargeRequest;
import com.strataguard.core.dto.billing.TenantChargeResponse;
import com.strataguard.core.dto.billing.UpdateTenantChargeRequest;
import com.strataguard.core.entity.TenantCharge;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TenantChargeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    TenantCharge toEntity(CreateTenantChargeRequest request);

    @Mapping(target = "estateName", ignore = true)
    TenantChargeResponse toResponse(TenantCharge tenantCharge);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenancyId", ignore = true)
    @Mapping(target = "estateId", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateTenantChargeRequest request, @MappingTarget TenantCharge tenantCharge);
}
