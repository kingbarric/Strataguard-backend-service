package com.strataguard.core.util;

import com.strataguard.core.dto.billing.CreateEstateChargeRequest;
import com.strataguard.core.dto.billing.EstateChargeResponse;
import com.strataguard.core.dto.billing.UpdateEstateChargeRequest;
import com.strataguard.core.entity.EstateCharge;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EstateChargeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    EstateCharge toEntity(CreateEstateChargeRequest request);

    @Mapping(target = "estateName", ignore = true)
    EstateChargeResponse toResponse(EstateCharge estateCharge);

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
    void updateEntity(UpdateEstateChargeRequest request, @MappingTarget EstateCharge estateCharge);
}
