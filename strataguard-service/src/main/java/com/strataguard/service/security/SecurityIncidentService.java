package com.strataguard.service.security;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.dto.security.*;
import com.strataguard.core.entity.SecurityIncident;
import com.strataguard.core.enums.IncidentStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.enums.ReporterType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.SecurityIncidentMapper;
import com.strataguard.infrastructure.repository.SecurityIncidentRepository;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SecurityIncidentService {

    private final SecurityIncidentRepository securityIncidentRepository;
    private final SecurityIncidentMapper securityIncidentMapper;
    private final NotificationService notificationService;

    public IncidentResponse reportIncident(UUID reportedBy, ReporterType reporterType, ReportIncidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        SecurityIncident incident = securityIncidentMapper.toEntity(request);
        incident.setTenantId(tenantId);
        incident.setReportedBy(reportedBy);
        incident.setReporterType(reporterType);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setIncidentNumber(generateIncidentNumber());

        SecurityIncident saved = securityIncidentRepository.save(incident);
        log.info("Reported security incident: {} for tenant: {}", saved.getIncidentNumber(), tenantId);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .type(NotificationType.INCIDENT_REPORTED)
                    .title("Security Incident: " + saved.getTitle())
                    .body("New incident reported: " + saved.getIncidentNumber())
                    .build());
        } catch (Exception e) {
            log.error("Failed to send incident reported notification", e);
        }

        return securityIncidentMapper.toResponse(saved);
    }

    public IncidentResponse updateIncident(UUID id, UpdateIncidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        SecurityIncident incident = securityIncidentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityIncident", "id", id));

        securityIncidentMapper.updateEntity(request, incident);
        SecurityIncident updated = securityIncidentRepository.save(incident);
        log.info("Updated security incident: {} for tenant: {}", id, tenantId);

        return securityIncidentMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        SecurityIncident incident = securityIncidentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityIncident", "id", id));

        return securityIncidentMapper.toResponse(incident);
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getAllIncidents(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<SecurityIncident> page = securityIncidentRepository.findAllByTenantId(tenantId, pageable);

        List<IncidentResponse> content = page.getContent().stream()
                .map(securityIncidentMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getIncidentsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<SecurityIncident> page = securityIncidentRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);

        List<IncidentResponse> content = page.getContent().stream()
                .map(securityIncidentMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getIncidentsByStatus(IncidentStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<SecurityIncident> page = securityIncidentRepository.findByStatusAndTenantId(status, tenantId, pageable);

        List<IncidentResponse> content = page.getContent().stream()
                .map(securityIncidentMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> searchIncidents(String query, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<SecurityIncident> page = securityIncidentRepository.search(tenantId, query, pageable);

        List<IncidentResponse> content = page.getContent().stream()
                .map(securityIncidentMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    public IncidentResponse assignIncident(UUID id, AssignIncidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        SecurityIncident incident = securityIncidentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityIncident", "id", id));

        incident.setAssignedTo(request.getAssignedTo());
        incident.setAssignedAt(Instant.now());

        if (incident.getStatus() == IncidentStatus.REPORTED) {
            incident.setStatus(IncidentStatus.INVESTIGATING);
        }

        SecurityIncident saved = securityIncidentRepository.save(incident);
        log.info("Assigned security incident: {} to staff: {} for tenant: {}", id, request.getAssignedTo(), tenantId);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .type(NotificationType.INCIDENT_ASSIGNED)
                    .title("Security Incident: " + incident.getTitle())
                    .body("Incident " + incident.getIncidentNumber() + " has been assigned")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send incident assigned notification", e);
        }

        return securityIncidentMapper.toResponse(saved);
    }

    public IncidentResponse resolveIncident(UUID id, UUID resolvedBy, ResolveIncidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        SecurityIncident incident = securityIncidentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityIncident", "id", id));

        incident.setResolvedAt(Instant.now());
        incident.setResolvedBy(resolvedBy);
        incident.setResolutionNotes(request.getResolutionNotes());
        incident.setStatus(IncidentStatus.RESOLVED);

        SecurityIncident saved = securityIncidentRepository.save(incident);
        log.info("Resolved security incident: {} by: {} for tenant: {}", id, resolvedBy, tenantId);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .type(NotificationType.INCIDENT_RESOLVED)
                    .title("Security Incident: " + incident.getTitle())
                    .body("Incident " + incident.getIncidentNumber() + " has been resolved")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send incident resolved notification", e);
        }

        return securityIncidentMapper.toResponse(saved);
    }

    public IncidentResponse closeIncident(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        SecurityIncident incident = securityIncidentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityIncident", "id", id));

        incident.setClosedAt(Instant.now());
        incident.setStatus(IncidentStatus.CLOSED);

        SecurityIncident saved = securityIncidentRepository.save(incident);
        log.info("Closed security incident: {} for tenant: {}", id, tenantId);

        return securityIncidentMapper.toResponse(saved);
    }

    // --- Private helpers ---

    private String generateIncidentNumber() {
        UUID tenantId = TenantContext.requireTenantId();
        String prefix = "INC-" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = securityIncidentRepository.countByIncidentNumberPrefix(tenantId, prefix + "%");
        return prefix + String.format("%06d", count + 1);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<?> page, List<T> content) {
        return PagedResponse.<T>builder()
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
