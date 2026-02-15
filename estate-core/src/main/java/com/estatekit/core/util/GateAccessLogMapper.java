package com.estatekit.core.util;

import com.estatekit.core.dto.accesslog.GateAccessLogResponse;
import com.estatekit.core.entity.GateAccessLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GateAccessLogMapper {

    GateAccessLogResponse toResponse(GateAccessLog log);
}
