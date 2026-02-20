package com.strataguard.service.security;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.BulkNotificationRequest;
import com.strataguard.core.dto.security.*;
import com.strataguard.core.entity.CameraStatusLog;
import com.strataguard.core.entity.CctvCamera;
import com.strataguard.core.enums.CameraStatus;
import com.strataguard.core.enums.CameraZone;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.CameraStatusLogMapper;
import com.strataguard.core.util.CctvCameraMapper;
import com.strataguard.infrastructure.repository.CameraStatusLogRepository;
import com.strataguard.infrastructure.repository.CctvCameraRepository;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CctvCameraService {

    private final CctvCameraRepository cctvCameraRepository;
    private final CameraStatusLogRepository cameraStatusLogRepository;
    private final CctvCameraMapper cctvCameraMapper;
    private final CameraStatusLogMapper cameraStatusLogMapper;
    private final NotificationService notificationService;

    public CameraResponse createCamera(CreateCameraRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (cctvCameraRepository.existsByCameraCodeAndTenantId(request.getCameraCode(), tenantId)) {
            throw new IllegalArgumentException("Camera code already in use");
        }

        CctvCamera camera = cctvCameraMapper.toEntity(request);
        camera.setTenantId(tenantId);

        CctvCamera saved = cctvCameraRepository.save(camera);
        log.info("Created camera: {} for tenant: {}", saved.getId(), tenantId);
        return cctvCameraMapper.toResponse(saved);
    }

    public CameraResponse updateCamera(UUID id, UpdateCameraRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        CctvCamera camera = cctvCameraRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CctvCamera", "id", id));

        cctvCameraMapper.updateEntity(request, camera);
        CctvCamera updated = cctvCameraRepository.save(camera);
        log.info("Updated camera: {} for tenant: {}", id, tenantId);
        return cctvCameraMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public CameraResponse getCamera(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        CctvCamera camera = cctvCameraRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CctvCamera", "id", id));

        return cctvCameraMapper.toResponse(camera);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CameraResponse> getCamerasByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<CctvCamera> page = cctvCameraRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);

        List<CameraResponse> content = page.getContent().stream()
                .map(cctvCameraMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CameraResponse> getCamerasByZone(UUID estateId, CameraZone zone, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<CctvCamera> page = cctvCameraRepository.findByZoneAndEstateIdAndTenantId(zone, estateId, tenantId, pageable);

        List<CameraResponse> content = page.getContent().stream()
                .map(cctvCameraMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    public CameraResponse updateCameraStatus(UUID id, UpdateCameraStatusRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        CctvCamera camera = cctvCameraRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CctvCamera", "id", id));

        CameraStatus oldStatus = camera.getStatus();
        CameraStatus newStatus = request.getNewStatus();

        camera.setStatus(newStatus);
        if (newStatus == CameraStatus.ONLINE) {
            camera.setLastOnlineAt(Instant.now());
        }
        cctvCameraRepository.save(camera);

        // Create status log
        CameraStatusLog statusLog = new CameraStatusLog();
        statusLog.setCameraId(camera.getId());
        statusLog.setPreviousStatus(oldStatus);
        statusLog.setNewStatus(newStatus);
        statusLog.setChangedAt(Instant.now());
        statusLog.setReason(request.getReason());
        statusLog.setTenantId(tenantId);
        cameraStatusLogRepository.save(statusLog);

        log.info("Camera {} status changed from {} to {} for tenant: {}", id, oldStatus, newStatus, tenantId);

        // Send notifications for status changes
        try {
            if (newStatus == CameraStatus.OFFLINE) {
                notificationService.sendBulk(BulkNotificationRequest.builder()
                        .estateId(camera.getEstateId())
                        .type(NotificationType.CAMERA_OFFLINE)
                        .title("Camera Offline")
                        .body("Camera " + camera.getCameraName() + " (" + camera.getCameraCode() + ") is now offline.")
                        .data(Map.of(
                                "cameraId", camera.getId().toString(),
                                "cameraName", camera.getCameraName(),
                                "cameraCode", camera.getCameraCode(),
                                "reason", request.getReason() != null ? request.getReason() : ""
                        ))
                        .build());
            } else if (newStatus == CameraStatus.ONLINE && oldStatus == CameraStatus.OFFLINE) {
                notificationService.sendBulk(BulkNotificationRequest.builder()
                        .estateId(camera.getEstateId())
                        .type(NotificationType.CAMERA_ONLINE)
                        .title("Camera Back Online")
                        .body("Camera " + camera.getCameraName() + " (" + camera.getCameraCode() + ") is back online.")
                        .data(Map.of(
                                "cameraId", camera.getId().toString(),
                                "cameraName", camera.getCameraName(),
                                "cameraCode", camera.getCameraCode()
                        ))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to send camera status notification for camera {}: {}", id, e.getMessage());
        }

        return cctvCameraMapper.toResponse(camera);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CameraStatusLogResponse> getStatusLogs(UUID cameraId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify camera exists
        cctvCameraRepository.findByIdAndTenantId(cameraId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CctvCamera", "id", cameraId));

        Page<CameraStatusLog> page = cameraStatusLogRepository.findByCameraIdAndTenantId(cameraId, tenantId, pageable);

        List<CameraStatusLogResponse> content = page.getContent().stream()
                .map(cameraStatusLogMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    public void deleteCamera(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        CctvCamera camera = cctvCameraRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CctvCamera", "id", id));

        camera.setDeleted(true);
        cctvCameraRepository.save(camera);
        log.info("Soft-deleted camera: {} for tenant: {}", id, tenantId);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<?> page, java.util.List<T> content) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
