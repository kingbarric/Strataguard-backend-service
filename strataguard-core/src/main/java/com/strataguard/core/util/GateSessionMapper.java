package com.strataguard.core.util;

import com.strataguard.core.dto.gate.GateSessionResponse;
import com.strataguard.core.entity.GateSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GateSessionMapper {

    GateSessionResponse toResponse(GateSession session);
}
