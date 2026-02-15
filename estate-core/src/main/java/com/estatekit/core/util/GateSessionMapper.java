package com.estatekit.core.util;

import com.estatekit.core.dto.gate.GateSessionResponse;
import com.estatekit.core.entity.GateSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GateSessionMapper {

    GateSessionResponse toResponse(GateSession session);
}
