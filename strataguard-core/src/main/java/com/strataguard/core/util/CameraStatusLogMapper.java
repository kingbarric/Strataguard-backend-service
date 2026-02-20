package com.strataguard.core.util;

import com.strataguard.core.dto.security.CameraStatusLogResponse;
import com.strataguard.core.entity.CameraStatusLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CameraStatusLogMapper {

    CameraStatusLogResponse toResponse(CameraStatusLog log);
}
