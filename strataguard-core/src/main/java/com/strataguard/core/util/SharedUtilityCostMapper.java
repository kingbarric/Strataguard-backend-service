package com.strataguard.core.util;

import com.strataguard.core.dto.utility.SharedUtilityCostResponse;
import com.strataguard.core.entity.SharedUtilityCost;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SharedUtilityCostMapper {

    SharedUtilityCostResponse toResponse(SharedUtilityCost cost);
}
