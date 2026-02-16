package com.strataguard.core.util;

import com.strataguard.core.dto.accesslog.GateAccessLogResponse;
import com.strataguard.core.entity.GateAccessLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GateAccessLogMapper {

    GateAccessLogResponse toResponse(GateAccessLog log);
}
