package com.strataguard.core.util;

import com.strataguard.core.dto.audit.AuditLogResponse;
import com.strataguard.core.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
