package com.estatekit.core.util;

import com.estatekit.core.dto.visitor.VisitPassResponse;
import com.estatekit.core.entity.VisitPass;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VisitPassMapper {

    VisitPassResponse toResponse(VisitPass visitPass);
}
