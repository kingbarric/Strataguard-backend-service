package com.strataguard.core.util;

import com.strataguard.core.dto.security.PatrolScanResponse;
import com.strataguard.core.dto.security.PatrolSessionResponse;
import com.strataguard.core.entity.PatrolScan;
import com.strataguard.core.entity.PatrolSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatrolSessionMapper {

    @Mapping(target = "scans", ignore = true)
    PatrolSessionResponse toResponse(PatrolSession session);

    PatrolScanResponse toScanResponse(PatrolScan scan);
}
