package com.strataguard.service.maintenance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.maintenance.*;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.entity.MaintenanceComment;
import com.strataguard.core.entity.MaintenanceRequest;
import com.strataguard.core.enums.MaintenancePriority;
import com.strataguard.core.enums.MaintenanceStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.MaintenanceCommentMapper;
import com.strataguard.core.util.MaintenanceMapper;
import com.strataguard.infrastructure.repository.*;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaintenanceService {

    private final MaintenanceRequestRepository requestRepository;
    private final MaintenanceCommentRepository commentRepository;
    private final ResidentRepository residentRepository;
    private final UnitRepository unitRepository;
    private final NotificationService notificationService;
    private final MaintenanceMapper maintenanceMapper;
    private final MaintenanceCommentMapper commentMapper;

    @Value("${maintenance.sla.urgent-hours:4}")
    private int slaUrgentHours;

    @Value("${maintenance.sla.high-hours:24}")
    private int slaHighHours;

    @Value("${maintenance.sla.medium-hours:72}")
    private int slaMediumHours;

    @Value("${maintenance.sla.low-hours:168}")
    private int slaLowHours;

    public MaintenanceResponse createRequest(UUID residentId, CreateMaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest();
        maintenanceRequest.setTenantId(tenantId);
        maintenanceRequest.setRequestNumber(generateRequestNumber(tenantId));
        maintenanceRequest.setUnitId(request.getUnitId());
        maintenanceRequest.setEstateId(request.getEstateId());
        maintenanceRequest.setResidentId(residentId);
        maintenanceRequest.setTitle(request.getTitle());
        maintenanceRequest.setDescription(request.getDescription());
        maintenanceRequest.setCategory(request.getCategory());
        maintenanceRequest.setPriority(request.getPriority());
        maintenanceRequest.setStatus(MaintenanceStatus.OPEN);
        maintenanceRequest.setPhotoUrls(request.getPhotoUrls());
        maintenanceRequest.setSlaDeadline(computeSlaDeadline(request.getPriority()));

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Created maintenance request: {} for resident: {} tenant: {}", saved.getRequestNumber(), residentId, tenantId);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(residentId)
                    .type(NotificationType.MAINTENANCE_CREATED)
                    .title("Maintenance Request Created")
                    .body("Your maintenance request " + saved.getRequestNumber() + " has been created: " + saved.getTitle())
                    .data(Map.of("requestId", saved.getId().toString(), "requestNumber", saved.getRequestNumber()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send maintenance creation notification: {}", e.getMessage());
        }

        return enrichResponse(saved);
    }

    public MaintenanceResponse updateRequest(UUID requestId, UpdateMaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        if (request.getTitle() != null) maintenanceRequest.setTitle(request.getTitle());
        if (request.getDescription() != null) maintenanceRequest.setDescription(request.getDescription());
        if (request.getCategory() != null) maintenanceRequest.setCategory(request.getCategory());
        if (request.getPriority() != null) {
            maintenanceRequest.setPriority(request.getPriority());
            maintenanceRequest.setSlaDeadline(computeSlaDeadline(request.getPriority()));
        }
        if (request.getPhotoUrls() != null) maintenanceRequest.setPhotoUrls(request.getPhotoUrls());

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Updated maintenance request: {} for tenant: {}", requestId, tenantId);
        return enrichResponse(saved);
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getRequest(UUID requestId) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest request = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));
        return enrichResponse(request);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceResponse> getAllRequests(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<MaintenanceRequest> page = requestRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceResponse> getMyRequests(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<MaintenanceRequest> page = requestRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceResponse> getRequestsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<MaintenanceRequest> page = requestRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceResponse> getRequestsByStatus(MaintenanceStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<MaintenanceRequest> page = requestRepository.findByStatusAndTenantId(status, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceResponse> searchRequests(String query, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<MaintenanceRequest> page = requestRepository.search(tenantId, query, pageable);
        return toPagedResponse(page);
    }

    public MaintenanceResponse assignRequest(UUID requestId, AssignMaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        maintenanceRequest.setAssignedTo(request.getAssignedTo());
        maintenanceRequest.setAssignedToPhone(request.getAssignedToPhone());
        maintenanceRequest.setAssignedAt(Instant.now());
        maintenanceRequest.setStatus(MaintenanceStatus.ASSIGNED);

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Assigned maintenance request {} to {}", requestId, request.getAssignedTo());

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(saved.getResidentId())
                    .type(NotificationType.MAINTENANCE_ASSIGNED)
                    .title("Maintenance Request Assigned")
                    .body("Your request " + saved.getRequestNumber() + " has been assigned to " + request.getAssignedTo())
                    .data(Map.of("requestId", saved.getId().toString(), "assignee", request.getAssignedTo()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send assignment notification: {}", e.getMessage());
        }

        return enrichResponse(saved);
    }

    public MaintenanceResponse submitCostEstimate(UUID requestId, CostEstimateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        maintenanceRequest.setEstimatedCost(request.getEstimatedCost());
        maintenanceRequest.setStatus(MaintenanceStatus.COST_ESTIMATE);

        if (request.getNotes() != null) {
            maintenanceRequest.setResolutionNotes(request.getNotes());
        }

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Cost estimate submitted for request {}: {}", requestId, request.getEstimatedCost());

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(saved.getResidentId())
                    .type(NotificationType.MAINTENANCE_COST_ESTIMATE)
                    .title("Cost Estimate for Maintenance")
                    .body("A cost estimate of " + request.getEstimatedCost() + " has been submitted for request " + saved.getRequestNumber() + ". Please approve to proceed.")
                    .data(Map.of("requestId", saved.getId().toString(), "estimatedCost", request.getEstimatedCost().toString()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send cost estimate notification: {}", e.getMessage());
        }

        return enrichResponse(saved);
    }

    public MaintenanceResponse approveCostEstimate(UUID requestId, String approvedBy) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        if (maintenanceRequest.getStatus() != MaintenanceStatus.COST_ESTIMATE) {
            throw new IllegalStateException("Request is not in COST_ESTIMATE status");
        }

        maintenanceRequest.setCostApprovedBy(approvedBy);
        maintenanceRequest.setCostApprovedAt(Instant.now());
        maintenanceRequest.setStatus(MaintenanceStatus.APPROVED);

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Cost estimate approved for request {} by {}", requestId, approvedBy);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(saved.getResidentId())
                    .type(NotificationType.MAINTENANCE_COST_APPROVED)
                    .title("Cost Estimate Approved")
                    .body("The cost estimate for request " + saved.getRequestNumber() + " has been approved. Work will begin shortly.")
                    .data(Map.of("requestId", saved.getId().toString()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send cost approval notification: {}", e.getMessage());
        }

        return enrichResponse(saved);
    }

    public MaintenanceResponse resolveRequest(UUID requestId, String resolutionNotes, java.math.BigDecimal actualCost) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        maintenanceRequest.setStatus(MaintenanceStatus.RESOLVED);
        maintenanceRequest.setResolvedAt(Instant.now());
        if (resolutionNotes != null) maintenanceRequest.setResolutionNotes(resolutionNotes);
        if (actualCost != null) maintenanceRequest.setActualCost(actualCost);

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Resolved maintenance request: {}", requestId);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(saved.getResidentId())
                    .type(NotificationType.MAINTENANCE_RESOLVED)
                    .title("Maintenance Request Resolved")
                    .body("Your maintenance request " + saved.getRequestNumber() + " has been resolved. Please rate the service.")
                    .data(Map.of("requestId", saved.getId().toString()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send resolution notification: {}", e.getMessage());
        }

        return enrichResponse(saved);
    }

    public MaintenanceResponse closeRequest(UUID requestId) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        maintenanceRequest.setStatus(MaintenanceStatus.CLOSED);
        maintenanceRequest.setClosedAt(Instant.now());

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Closed maintenance request: {}", requestId);
        return enrichResponse(saved);
    }

    public MaintenanceResponse cancelRequest(UUID requestId) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        maintenanceRequest.setStatus(MaintenanceStatus.CANCELLED);

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Cancelled maintenance request: {}", requestId);
        return enrichResponse(saved);
    }

    public MaintenanceResponse rateRequest(UUID requestId, UUID residentId, RateMaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        if (!maintenanceRequest.getResidentId().equals(residentId)) {
            throw new IllegalStateException("Only the requesting resident can rate this request");
        }

        if (maintenanceRequest.getStatus() != MaintenanceStatus.RESOLVED && maintenanceRequest.getStatus() != MaintenanceStatus.CLOSED) {
            throw new IllegalStateException("Can only rate resolved or closed requests");
        }

        maintenanceRequest.setSatisfactionRating(request.getRating());
        maintenanceRequest.setSatisfactionComment(request.getComment());

        MaintenanceRequest saved = requestRepository.save(maintenanceRequest);
        log.info("Rated maintenance request {} with {} stars", requestId, request.getRating());
        return enrichResponse(saved);
    }

    public CommentResponse addComment(UUID requestId, UUID authorId, String authorName, String authorRole, AddCommentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", "id", requestId));

        MaintenanceComment comment = new MaintenanceComment();
        comment.setTenantId(tenantId);
        comment.setRequestId(requestId);
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment.setAuthorRole(authorRole);
        comment.setContent(request.getContent());
        comment.setAttachmentUrls(request.getAttachmentUrls());
        comment.setInternal(request.isInternal());

        MaintenanceComment saved = commentRepository.save(comment);
        log.info("Added comment to request {} by {}", requestId, authorName);

        // Notify resident of public comments (if not internal and not by the resident)
        if (!request.isInternal()) {
            MaintenanceRequest maintenanceRequest = requestRepository.findByIdAndTenantId(requestId, tenantId).orElse(null);
            if (maintenanceRequest != null && maintenanceRequest.getResidentId() != null && !maintenanceRequest.getResidentId().equals(authorId)) {
                try {
                    notificationService.send(SendNotificationRequest.builder()
                            .recipientId(maintenanceRequest.getResidentId())
                            .type(NotificationType.MAINTENANCE_COMMENT)
                            .title("New Comment on Maintenance Request")
                            .body(authorName + " commented on your request " + maintenanceRequest.getRequestNumber())
                            .data(Map.of("requestId", requestId.toString()))
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to send comment notification: {}", e.getMessage());
                }
            }
        }

        return commentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> getComments(UUID requestId, boolean includeInternal, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<MaintenanceComment> page;
        if (includeInternal) {
            page = commentRepository.findByRequestIdAndTenantId(requestId, tenantId, pageable);
        } else {
            page = commentRepository.findPublicByRequestIdAndTenantId(requestId, tenantId, pageable);
        }
        return PagedResponse.<CommentResponse>builder()
                .content(page.getContent().stream().map(commentMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void checkSlaBreaches() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return;

        List<MaintenanceRequest> breached = requestRepository.findSlaBreachedRequests(Instant.now(), tenantId);
        for (MaintenanceRequest request : breached) {
            request.setSlaBreached(true);
            request.setEscalated(true);
            request.setEscalatedAt(Instant.now());
            requestRepository.save(request);

            try {
                notificationService.send(SendNotificationRequest.builder()
                        .recipientId(request.getResidentId())
                        .type(NotificationType.MAINTENANCE_SLA_BREACH)
                        .title("SLA Breach Alert")
                        .body("Maintenance request " + request.getRequestNumber() + " has breached its SLA deadline")
                        .data(Map.of("requestId", request.getId().toString(), "requestNumber", request.getRequestNumber()))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to send SLA breach notification: {}", e.getMessage());
            }
        }

        if (!breached.isEmpty()) {
            log.warn("Detected {} SLA breaches for tenant {}", breached.size(), tenantId);
        }
    }

    @Transactional(readOnly = true)
    public MaintenanceDashboardResponse getDashboard() {
        UUID tenantId = TenantContext.requireTenantId();
        Double avgRating = requestRepository.averageSatisfactionRatingByTenantId(tenantId);

        return MaintenanceDashboardResponse.builder()
                .totalRequests(requestRepository.countByTenantId(tenantId))
                .openRequests(requestRepository.countByStatusAndTenantId(MaintenanceStatus.OPEN, tenantId))
                .assignedRequests(requestRepository.countByStatusAndTenantId(MaintenanceStatus.ASSIGNED, tenantId))
                .inProgressRequests(requestRepository.countByStatusAndTenantId(MaintenanceStatus.IN_PROGRESS, tenantId))
                .resolvedRequests(requestRepository.countByStatusAndTenantId(MaintenanceStatus.RESOLVED, tenantId))
                .slaBreachedRequests(requestRepository.countSlaBreachedByTenantId(tenantId))
                .averageSatisfactionRating(avgRating != null ? avgRating : 0.0)
                .build();
    }

    private Instant computeSlaDeadline(MaintenancePriority priority) {
        int hours = switch (priority) {
            case URGENT -> slaUrgentHours;
            case HIGH -> slaHighHours;
            case MEDIUM -> slaMediumHours;
            case LOW -> slaLowHours;
        };
        return Instant.now().plus(hours, ChronoUnit.HOURS);
    }

    private String generateRequestNumber(UUID tenantId) {
        YearMonth yearMonth = YearMonth.now();
        String prefix = String.format("MR-%d%02d-", yearMonth.getYear(), yearMonth.getMonthValue());
        long count = requestRepository.countByRequestNumberPrefix(tenantId, prefix + "%");
        return String.format("%s%06d", prefix, count + 1);
    }

    private MaintenanceResponse enrichResponse(MaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        MaintenanceResponse response = maintenanceMapper.toResponse(request);

        if (request.getUnitId() != null) {
            unitRepository.findByIdAndTenantId(request.getUnitId(), tenantId)
                    .ifPresent(u -> response.setUnitNumber(u.getUnitNumber()));
        }

        residentRepository.findByIdAndTenantId(request.getResidentId(), tenantId)
                .ifPresent(r -> response.setResidentName(r.getFirstName() + " " + r.getLastName()));

        return response;
    }

    private PagedResponse<MaintenanceResponse> toPagedResponse(Page<MaintenanceRequest> page) {
        return PagedResponse.<MaintenanceResponse>builder()
                .content(page.getContent().stream().map(this::enrichResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
