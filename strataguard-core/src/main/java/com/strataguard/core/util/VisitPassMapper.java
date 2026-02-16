package com.strataguard.core.util;

import com.strataguard.core.dto.visitor.VisitPassResponse;
import com.strataguard.core.entity.VisitPass;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VisitPassMapper {

    VisitPassResponse toResponse(VisitPass visitPass);
}
