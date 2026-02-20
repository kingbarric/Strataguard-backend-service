package com.strataguard.core.dto.audit;

import com.strataguard.core.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterRequest {
    private String actorId;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private Instant startDate;
    private Instant endDate;
}
