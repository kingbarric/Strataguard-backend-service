package com.strataguard.service.security;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.security.*;
import com.strataguard.core.entity.PatrolCheckpoint;
import com.strataguard.core.entity.PatrolScan;
import com.strataguard.core.entity.PatrolSession;
import com.strataguard.core.entity.Staff;
import com.strataguard.core.enums.PatrolSessionStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.PatrolCheckpointMapper;
import com.strataguard.core.util.PatrolSessionMapper;
import com.strataguard.infrastructure.repository.PatrolCheckpointRepository;
import com.strataguard.infrastructure.repository.PatrolScanRepository;
import com.strataguard.infrastructure.repository.PatrolSessionRepository;
import com.strataguard.infrastructure.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatrolService {

    private final PatrolCheckpointRepository checkpointRepository;
    private final PatrolSessionRepository sessionRepository;
    private final PatrolScanRepository scanRepository;
    private final StaffRepository staffRepository;
    private final PatrolCheckpointMapper checkpointMapper;
    private final PatrolSessionMapper sessionMapper;

    // ──────────────────────────────────────────────
    // Checkpoint CRUD
    // ──────────────────────────────────────────────

    public CheckpointResponse createCheckpoint(CreateCheckpointRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (checkpointRepository.existsByQrCodeAndTenantId(request.getQrCode(), tenantId)) {
            throw new IllegalArgumentException("QR code already in use");
        }

        PatrolCheckpoint checkpoint = checkpointMapper.toEntity(request);
        checkpoint.setTenantId(tenantId);

        PatrolCheckpoint saved = checkpointRepository.save(checkpoint);
        log.info("Created patrol checkpoint: {} for estate: {}", saved.getId(), saved.getEstateId());
        return checkpointMapper.toResponse(saved);
    }

    public CheckpointResponse updateCheckpoint(UUID id, UpdateCheckpointRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        PatrolCheckpoint checkpoint = checkpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolCheckpoint", "id", id));

        checkpointMapper.updateEntity(request, checkpoint);

        PatrolCheckpoint saved = checkpointRepository.save(checkpoint);
        log.info("Updated patrol checkpoint: {}", saved.getId());
        return checkpointMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CheckpointResponse> getCheckpointsByEstate(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();

        return checkpointRepository.findByEstateIdAndTenantId(estateId, tenantId).stream()
                .map(checkpointMapper::toResponse)
                .toList();
    }

    public void deleteCheckpoint(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        PatrolCheckpoint checkpoint = checkpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolCheckpoint", "id", id));

        checkpoint.setDeleted(true);
        checkpointRepository.save(checkpoint);
        log.info("Soft-deleted patrol checkpoint: {}", id);
    }

    // ──────────────────────────────────────────────
    // Patrol Session
    // ──────────────────────────────────────────────

    public PatrolSessionResponse startPatrol(UUID staffId, StartPatrolRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId));

        if (!request.getEstateId().equals(staff.getEstateId())) {
            throw new IllegalArgumentException("Staff does not belong to the specified estate");
        }

        sessionRepository.findInProgressByStaffIdAndTenantId(staffId, tenantId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Staff already has an in-progress patrol");
                });

        PatrolSession session = new PatrolSession();
        session.setStaffId(staffId);
        session.setEstateId(request.getEstateId());
        session.setTenantId(tenantId);
        session.setStartedAt(Instant.now());
        session.setStatus(PatrolSessionStatus.IN_PROGRESS);
        session.setTotalCheckpoints(checkpointRepository.countActiveByEstateIdAndTenantId(request.getEstateId(), tenantId));
        session.setScannedCheckpoints(0);
        session.setCompletionPercentage(0.0);
        session.setNotes(request.getNotes());

        PatrolSession saved = sessionRepository.save(session);
        log.info("Started patrol session: {} by staff: {} for estate: {}", saved.getId(), staffId, request.getEstateId());
        return sessionMapper.toResponse(saved);
    }

    public PatrolSessionResponse scanCheckpoint(UUID sessionId, PatrolScanRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        PatrolSession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolSession", "id", sessionId));

        if (session.getStatus() != PatrolSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Patrol is not in progress");
        }

        PatrolCheckpoint checkpoint = checkpointRepository.findByQrCodeAndTenantId(request.getQrCode(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolCheckpoint", "qrCode", request.getQrCode()));

        if (!checkpoint.getEstateId().equals(session.getEstateId())) {
            throw new IllegalArgumentException("Checkpoint does not belong to the same estate as the patrol session");
        }

        if (scanRepository.existsBySessionIdAndCheckpointIdAndTenantId(sessionId, checkpoint.getId(), tenantId)) {
            throw new IllegalArgumentException("Checkpoint already scanned in this patrol");
        }

        PatrolScan scan = new PatrolScan();
        scan.setSessionId(sessionId);
        scan.setCheckpointId(checkpoint.getId());
        scan.setScannedAt(Instant.now());
        scan.setLatitude(request.getLatitude());
        scan.setLongitude(request.getLongitude());
        scan.setNotes(request.getNotes());
        scan.setPhotoUrl(request.getPhotoUrl());
        scan.setTenantId(tenantId);

        scanRepository.save(scan);

        session.setScannedCheckpoints(session.getScannedCheckpoints() + 1);
        if (session.getTotalCheckpoints() > 0) {
            session.setCompletionPercentage(
                    (double) session.getScannedCheckpoints() / session.getTotalCheckpoints() * 100
            );
        }
        PatrolSession updatedSession = sessionRepository.save(session);

        log.info("Scanned checkpoint: {} in session: {}", checkpoint.getId(), sessionId);

        PatrolSessionResponse response = sessionMapper.toResponse(updatedSession);
        List<PatrolScan> scans = scanRepository.findBySessionIdAndTenantId(sessionId, tenantId);
        response.setScans(scans.stream().map(sessionMapper::toScanResponse).toList());
        return response;
    }

    public PatrolSessionResponse completePatrol(UUID sessionId) {
        UUID tenantId = TenantContext.requireTenantId();

        PatrolSession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolSession", "id", sessionId));

        if (session.getStatus() != PatrolSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Patrol is not in progress");
        }

        session.setStatus(PatrolSessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());

        PatrolSession saved = sessionRepository.save(session);
        log.info("Completed patrol session: {}", sessionId);
        return sessionMapper.toResponse(saved);
    }

    public PatrolSessionResponse abandonPatrol(UUID sessionId) {
        UUID tenantId = TenantContext.requireTenantId();

        PatrolSession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolSession", "id", sessionId));

        if (session.getStatus() != PatrolSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Patrol is not in progress");
        }

        session.setStatus(PatrolSessionStatus.ABANDONED);
        session.setCompletedAt(Instant.now());

        PatrolSession saved = sessionRepository.save(session);
        log.info("Abandoned patrol session: {}", sessionId);
        return sessionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatrolSessionResponse getPatrolSession(UUID sessionId) {
        UUID tenantId = TenantContext.requireTenantId();

        PatrolSession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PatrolSession", "id", sessionId));

        PatrolSessionResponse response = sessionMapper.toResponse(session);
        List<PatrolScan> scans = scanRepository.findBySessionIdAndTenantId(sessionId, tenantId);
        response.setScans(scans.stream().map(sessionMapper::toScanResponse).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PatrolSessionResponse> getPatrolsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<PatrolSession> page = sessionRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        List<PatrolSessionResponse> content = page.getContent().stream()
                .map(sessionMapper::toResponse)
                .toList();
        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PatrolSessionResponse> getMyPatrols(UUID staffId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<PatrolSession> page = sessionRepository.findByStaffIdAndTenantId(staffId, tenantId, pageable);
        List<PatrolSessionResponse> content = page.getContent().stream()
                .map(sessionMapper::toResponse)
                .toList();
        return toPagedResponse(page, content);
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

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
