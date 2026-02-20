package com.strataguard.service.security;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.dto.security.EmergencyAlertResponse;
import com.strataguard.core.dto.security.ResolveEmergencyRequest;
import com.strataguard.core.dto.security.TriggerEmergencyRequest;
import com.strataguard.core.entity.EmergencyAlert;
import com.strataguard.core.enums.EmergencyAlertStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.EmergencyAlertRepository;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmergencyAlertService {

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final NotificationService notificationService;

    public EmergencyAlertResponse triggerAlert(UUID residentId, TriggerEmergencyRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        EmergencyAlert alert = new EmergencyAlert();
        alert.setTenantId(tenantId);
        alert.setResidentId(residentId);
        alert.setEstateId(request.getEstateId());
        alert.setUnitId(request.getUnitId());
        alert.setAlertType(request.getAlertType());
        alert.setDescription(request.getDescription());
        alert.setLatitude(request.getLatitude());
        alert.setLongitude(request.getLongitude());
        alert.setStatus(EmergencyAlertStatus.TRIGGERED);

        alert = emergencyAlertRepository.save(alert);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .type(NotificationType.EMERGENCY_ALERT_TRIGGERED)
                    .title("Emergency Alert: " + alert.getAlertType())
                    .body("Emergency alert triggered at estate")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send emergency notification", e);
        }

        log.info("Emergency alert {} triggered by resident {} in estate {}",
                alert.getId(), residentId, request.getEstateId());

        return toResponse(alert);
    }

    public EmergencyAlertResponse acknowledgeAlert(UUID alertId, UUID staffId) {
        UUID tenantId = TenantContext.requireTenantId();

        EmergencyAlert alert = emergencyAlertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", "id", alertId));

        if (alert.getStatus() != EmergencyAlertStatus.TRIGGERED) {
            throw new IllegalStateException("Alert must be in TRIGGERED status to acknowledge");
        }

        alert.setAcknowledgedBy(staffId);
        alert.setAcknowledgedAt(Instant.now());
        alert.setStatus(EmergencyAlertStatus.ACKNOWLEDGED);
        alert.setResponseTimeSeconds(Duration.between(alert.getCreatedAt(), Instant.now()).getSeconds());

        alert = emergencyAlertRepository.save(alert);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(alert.getResidentId())
                    .type(NotificationType.EMERGENCY_ALERT_ACKNOWLEDGED)
                    .title("Emergency Alert Acknowledged")
                    .body("Your emergency alert has been acknowledged and help is on the way")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send acknowledgement notification for alert {}", alertId, e);
        }

        log.info("Emergency alert {} acknowledged by staff {}", alertId, staffId);

        return toResponse(alert);
    }

    public EmergencyAlertResponse respondToAlert(UUID alertId, UUID staffId) {
        UUID tenantId = TenantContext.requireTenantId();

        EmergencyAlert alert = emergencyAlertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", "id", alertId));

        if (alert.getStatus() != EmergencyAlertStatus.ACKNOWLEDGED) {
            throw new IllegalStateException("Alert must be in ACKNOWLEDGED status to respond");
        }

        alert.setRespondedBy(staffId);
        alert.setRespondedAt(Instant.now());
        alert.setStatus(EmergencyAlertStatus.RESPONDING);

        alert = emergencyAlertRepository.save(alert);

        log.info("Emergency alert {} being responded to by staff {}", alertId, staffId);

        return toResponse(alert);
    }

    public EmergencyAlertResponse resolveAlert(UUID alertId, UUID staffId, ResolveEmergencyRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        EmergencyAlert alert = emergencyAlertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", "id", alertId));

        alert.setResolvedBy(staffId);
        alert.setResolvedAt(Instant.now());
        alert.setResolutionNotes(request.getResolutionNotes());
        alert.setStatus(EmergencyAlertStatus.RESOLVED);

        alert = emergencyAlertRepository.save(alert);

        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(alert.getResidentId())
                    .type(NotificationType.EMERGENCY_ALERT_RESOLVED)
                    .title("Emergency Alert Resolved")
                    .body("Your emergency alert has been resolved")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send resolution notification for alert {}", alertId, e);
        }

        log.info("Emergency alert {} resolved by staff {}", alertId, staffId);

        return toResponse(alert);
    }

    public EmergencyAlertResponse markFalseAlarm(UUID alertId) {
        UUID tenantId = TenantContext.requireTenantId();

        EmergencyAlert alert = emergencyAlertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", "id", alertId));

        alert.setStatus(EmergencyAlertStatus.FALSE_ALARM);

        alert = emergencyAlertRepository.save(alert);

        log.info("Emergency alert {} marked as false alarm", alertId);

        return toResponse(alert);
    }

    @Transactional(readOnly = true)
    public EmergencyAlertResponse getAlert(UUID alertId) {
        UUID tenantId = TenantContext.requireTenantId();

        EmergencyAlert alert = emergencyAlertRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", "id", alertId));

        return toResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<EmergencyAlertResponse> getActiveAlerts(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();

        return emergencyAlertRepository.findActiveByEstateIdAndTenantId(estateId, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmergencyAlertResponse> getAlertsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Page<EmergencyAlert> page = emergencyAlertRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);

        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmergencyAlertResponse> getMyAlerts(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Page<EmergencyAlert> page = emergencyAlertRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);

        return toPagedResponse(page);
    }

    private EmergencyAlertResponse toResponse(EmergencyAlert alert) {
        return EmergencyAlertResponse.builder()
                .id(alert.getId())
                .residentId(alert.getResidentId())
                .estateId(alert.getEstateId())
                .unitId(alert.getUnitId())
                .alertType(alert.getAlertType())
                .status(alert.getStatus())
                .description(alert.getDescription())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .respondedBy(alert.getRespondedBy())
                .respondedAt(alert.getRespondedAt())
                .resolvedBy(alert.getResolvedBy())
                .resolvedAt(alert.getResolvedAt())
                .resolutionNotes(alert.getResolutionNotes())
                .responseTimeSeconds(alert.getResponseTimeSeconds())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

    private PagedResponse<EmergencyAlertResponse> toPagedResponse(Page<EmergencyAlert> page) {
        return PagedResponse.<EmergencyAlertResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
