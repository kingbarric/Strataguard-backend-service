package com.strataguard.core.util;

import com.strataguard.core.dto.utility.UtilityReadingResponse;
import com.strataguard.core.entity.UtilityReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UtilityReadingMapper {

    @Mapping(target = "meterNumber", ignore = true)
    UtilityReadingResponse toResponse(UtilityReading reading);
}
