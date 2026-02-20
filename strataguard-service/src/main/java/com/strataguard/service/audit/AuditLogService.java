package com.strataguard.service.audit;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.audit.AuditLogFilterRequest;
import com.strataguard.core.dto.audit.AuditLogResponse;
import com.strataguard.core.dto.audit.AuditSummaryResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.AuditLog;
import com.strataguard.core.enums.AuditAction;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.AuditLogMapper;
import com.strataguard.infrastructure.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public void logEvent(UUID tenantId, String actorId, String actorName, AuditAction action,
                         String entityType, String entityId, String oldValue, String newValue,
                         String ipAddress, String userAgent, String description) {

        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(tenantId);
        auditLog.setActorId(actorId);
        auditLog.setActorName(actorName);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setDescription(description);
        auditLog.setTimestamp(Instant.now());

        // Hash chain: get previous hash
        Optional<AuditLog> lastLog = auditLogRepository.findLastByTenantId(tenantId);
        String previousHash = lastLog.map(AuditLog::getHash).orElse("GENESIS");
        auditLog.setPreviousHash(previousHash);

        // Compute hash for this entry
        String hash = computeHash(auditLog, previousHash);
        auditLog.setHash(hash);

        auditLogRepository.save(auditLog);
        log.debug("Audit log: {} {} {} {} for tenant: {}", actorId, action, entityType, entityId, tenantId);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLog(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        AuditLog auditLog = auditLogRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));

        return auditLogMapper.toResponse(auditLog);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAllLogs(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<AuditLog> page = auditLogRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getLogsByFilter(AuditLogFilterRequest filter, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Page<AuditLog> page;

        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            page = auditLogRepository.findByDateRange(tenantId, filter.getStartDate(), filter.getEndDate(), pageable);
        } else if (filter.getActorId() != null) {
            page = auditLogRepository.findByActorId(filter.getActorId(), tenantId, pageable);
        } else if (filter.getAction() != null) {
            page = auditLogRepository.findByAction(filter.getAction(), tenantId, pageable);
        } else if (filter.getEntityType() != null && filter.getEntityId() != null) {
            page = auditLogRepository.findByEntityTypeAndEntityId(filter.getEntityType(), filter.getEntityId(), tenantId, pageable);
        } else if (filter.getEntityType() != null) {
            page = auditLogRepository.findByEntityType(filter.getEntityType(), tenantId, pageable);
        } else {
            page = auditLogRepository.findAllByTenantId(tenantId, pageable);
        }

        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getEntityHistory(String entityType, String entityId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<AuditLog> page = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public AuditSummaryResponse getSummary() {
        UUID tenantId = TenantContext.requireTenantId();

        long totalEvents = auditLogRepository.countByTenantId(tenantId);

        Map<String, Long> eventsByAction = new LinkedHashMap<>();
        for (AuditAction action : AuditAction.values()) {
            long count = auditLogRepository.countByActionAndTenantId(action, tenantId);
            if (count > 0) {
                eventsByAction.put(action.name(), count);
            }
        }

        // Get top entity types
        String[] entityTypes = {"Estate", "Unit", "Resident", "Visitor", "Payment", "MaintenanceRequest", "Violation"};
        Map<String, Long> eventsByEntityType = new LinkedHashMap<>();
        for (String entityType : entityTypes) {
            long count = auditLogRepository.countByEntityTypeAndTenantId(entityType, tenantId);
            if (count > 0) {
                eventsByEntityType.put(entityType, count);
            }
        }

        return AuditSummaryResponse.builder()
                .totalEvents(totalEvents)
                .eventsByAction(eventsByAction)
                .eventsByEntityType(eventsByEntityType)
                .topActors(Map.of())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean verifyIntegrity(int limit) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<AuditLog> page = auditLogRepository.findLatest(tenantId, Pageable.ofSize(limit));
        List<AuditLog> logs = new ArrayList<>(page.getContent());
        Collections.reverse(logs);

        for (int i = 0; i < logs.size(); i++) {
            AuditLog current = logs.get(i);
            String expectedPreviousHash = (i == 0) ? current.getPreviousHash() : logs.get(i - 1).getHash();
            String recomputedHash = computeHash(current, expectedPreviousHash);

            if (!recomputedHash.equals(current.getHash())) {
                log.warn("Audit log integrity check failed at entry: {}", current.getId());
                return false;
            }
        }

        log.info("Audit log integrity verified for {} entries, tenant: {}", logs.size(), tenantId);
        return true;
    }

    String computeHash(AuditLog auditLog, String previousHash) {
        try {
            String data = String.join("|",
                    auditLog.getTenantId().toString(),
                    auditLog.getActorId(),
                    auditLog.getAction().name(),
                    auditLog.getEntityType(),
                    auditLog.getEntityId() != null ? auditLog.getEntityId() : "",
                    auditLog.getTimestamp().toString(),
                    previousHash != null ? previousHash : "");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private PagedResponse<AuditLogResponse> toPagedResponse(Page<AuditLog> page) {
        List<AuditLogResponse> content = page.getContent().stream()
                .map(auditLogMapper::toResponse)
                .toList();
        return PagedResponse.<AuditLogResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
