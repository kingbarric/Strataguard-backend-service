package com.strataguard.core.dto.audit;

import com.strataguard.core.enums.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private String actorId;
    private String actorName;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String description;
    private Instant timestamp;
    private String hash;
    private String previousHash;
}
