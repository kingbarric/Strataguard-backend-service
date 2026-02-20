package com.strataguard.core.util;

import com.strataguard.core.dto.utility.CreateUtilityMeterRequest;
import com.strataguard.core.dto.utility.UtilityMeterResponse;
import com.strataguard.core.entity.UtilityMeter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UtilityMeterMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "lastReadingValue", ignore = true)
    @Mapping(target = "lastReadingDate", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    UtilityMeter toEntity(CreateUtilityMeterRequest request);

    @Mapping(target = "unitNumber", ignore = true)
    UtilityMeterResponse toResponse(UtilityMeter meter);
}
