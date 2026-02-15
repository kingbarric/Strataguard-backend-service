package com.estatekit.service.gate;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.accesslog.GateAccessLogResponse;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.gate.*;
import com.estatekit.core.entity.GateAccessLog;
import com.estatekit.core.entity.GateSession;
import com.estatekit.core.entity.Resident;
import com.estatekit.core.entity.Vehicle;
import com.estatekit.core.enums.GateEventType;
import com.estatekit.core.enums.GateSessionStatus;
import com.estatekit.core.enums.VehicleStatus;
import com.estatekit.core.exception.GateAccessDeniedException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.GateAccessLogMapper;
import com.estatekit.core.util.GateSessionMapper;
import com.estatekit.infrastructure.repository.GateAccessLogRepository;
import com.estatekit.infrastructure.repository.GateSessionRepository;
import com.estatekit.infrastructure.repository.ResidentRepository;
import com.estatekit.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GateService {

    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;
    private final GateSessionRepository gateSessionRepository;
    private final GateAccessLogRepository gateAccessLogRepository;
    private final GateSessionMapper gateSessionMapper;
    private final GateAccessLogMapper gateAccessLogMapper;
    private final ExitPassService exitPassService;

    public GateEntryResponse processEntry(GateEntryRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String guardId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Lookup vehicle by QR sticker code
        Vehicle vehicle = vehicleRepository.findByQrStickerCodeAndTenantId(request.getQrStickerCode(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "qrStickerCode", request.getQrStickerCode()));

        // Validate vehicle is active
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            logEvent(null, vehicle.getId(), vehicle.getResidentId(), GateEventType.ENTRY_SCAN, guardId, "Vehicle not active: " + vehicle.getStatus(), false);
            throw new GateAccessDeniedException("Vehicle is not active. Current status: " + vehicle.getStatus());
        }

        // Check no duplicate open session
        gateSessionRepository.findByVehicleIdAndStatusAndTenantId(vehicle.getId(), GateSessionStatus.OPEN, tenantId)
                .ifPresent(s -> {
                    throw new GateAccessDeniedException("Vehicle already has an open gate session. Entry at: " + s.getEntryTime());
                });

        // Fetch resident details
        Resident resident = residentRepository.findByIdAndTenantId(vehicle.getResidentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", vehicle.getResidentId()));

        // Create gate session
        GateSession session = new GateSession();
        session.setTenantId(tenantId);
        session.setVehicleId(vehicle.getId());
        session.setResidentId(vehicle.getResidentId());
        session.setPlateNumber(vehicle.getPlateNumber());
        session.setStatus(GateSessionStatus.OPEN);
        session.setEntryTime(Instant.now());
        session.setEntryGuardId(guardId);
        session.setEntryNote(request.getNote());

        GateSession saved = gateSessionRepository.save(session);

        // Log entry event
        logEvent(saved.getId(), vehicle.getId(), vehicle.getResidentId(), GateEventType.ENTRY_SCAN, guardId, "Vehicle entered gate", true);

        log.info("Gate entry: vehicle={}, plate={}, session={}, guard={}, tenant={}",
                vehicle.getId(), vehicle.getPlateNumber(), saved.getId(), guardId, tenantId);

        return GateEntryResponse.builder()
                .sessionId(saved.getId())
                .status(saved.getStatus())
                .entryTime(saved.getEntryTime())
                .vehicleId(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .vehicleType(vehicle.getVehicleType())
                .vehicleStatus(vehicle.getStatus())
                .residentId(resident.getId())
                .residentFirstName(resident.getFirstName())
                .residentLastName(resident.getLastName())
                .residentPhone(resident.getPhone())
                .build();
    }

    public GateExitResponse processExit(GateExitRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String guardId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Lookup vehicle by QR sticker code
        Vehicle vehicle = vehicleRepository.findByQrStickerCodeAndTenantId(request.getQrStickerCode(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "qrStickerCode", request.getQrStickerCode()));

        // Find open session
        GateSession session = gateSessionRepository.findByVehicleIdAndStatusAndTenantId(vehicle.getId(), GateSessionStatus.OPEN, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("GateSession", "vehicleId (OPEN)", vehicle.getId()));

        // Validate exit pass token
        boolean valid = exitPassService.validateExitPass(request.getExitPassToken(), vehicle.getId(), tenantId);
        if (!valid) {
            logEvent(session.getId(), vehicle.getId(), vehicle.getResidentId(), GateEventType.EXIT_PASS_FAILED, guardId, "Invalid or expired exit pass", false);
            throw new GateAccessDeniedException("Invalid or expired exit pass token");
        }

        logEvent(session.getId(), vehicle.getId(), vehicle.getResidentId(), GateEventType.EXIT_PASS_VALIDATED, guardId, "Exit pass validated", true);

        // Close session
        session.setStatus(GateSessionStatus.CLOSED);
        session.setExitTime(Instant.now());
        session.setExitGuardId(guardId);
        session.setExitNote(request.getNote());
        GateSession updated = gateSessionRepository.save(session);

        logEvent(session.getId(), vehicle.getId(), vehicle.getResidentId(), GateEventType.EXIT_SCAN, guardId, "Vehicle exited gate", true);

        Resident resident = residentRepository.findByIdAndTenantId(vehicle.getResidentId(), tenantId).orElse(null);

        log.info("Gate exit: vehicle={}, plate={}, session={}, guard={}, tenant={}",
                vehicle.getId(), vehicle.getPlateNumber(), session.getId(), guardId, tenantId);

        return GateExitResponse.builder()
                .sessionId(updated.getId())
                .status(updated.getStatus())
                .entryTime(updated.getEntryTime())
                .exitTime(updated.getExitTime())
                .vehicleId(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .residentId(vehicle.getResidentId())
                .residentFirstName(resident != null ? resident.getFirstName() : null)
                .residentLastName(resident != null ? resident.getLastName() : null)
                .build();
    }

    public GateExitResponse processRemoteApprovalExit(UUID sessionId) {
        UUID tenantId = TenantContext.requireTenantId();
        String guardId = SecurityContextHolder.getContext().getAuthentication().getName();

        GateSession session = gateSessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("GateSession", "id", sessionId));

        if (session.getStatus() != GateSessionStatus.OPEN) {
            throw new GateAccessDeniedException("Session is not open. Current status: " + session.getStatus());
        }

        // Close session
        session.setStatus(GateSessionStatus.CLOSED);
        session.setExitTime(Instant.now());
        session.setExitGuardId(guardId);
        GateSession updated = gateSessionRepository.save(session);

        logEvent(session.getId(), session.getVehicleId(), session.getResidentId(), GateEventType.EXIT_SCAN, guardId, "Vehicle exited via remote approval", true);

        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(session.getVehicleId(), tenantId).orElse(null);
        Resident resident = residentRepository.findByIdAndTenantId(session.getResidentId(), tenantId).orElse(null);

        log.info("Gate exit (remote approval): session={}, guard={}, tenant={}", sessionId, guardId, tenantId);

        return GateExitResponse.builder()
                .sessionId(updated.getId())
                .status(updated.getStatus())
                .entryTime(updated.getEntryTime())
                .exitTime(updated.getExitTime())
                .vehicleId(session.getVehicleId())
                .plateNumber(session.getPlateNumber())
                .residentId(session.getResidentId())
                .residentFirstName(resident != null ? resident.getFirstName() : null)
                .residentLastName(resident != null ? resident.getLastName() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public GateSessionResponse getSession(UUID sessionId) {
        UUID tenantId = TenantContext.requireTenantId();
        GateSession session = gateSessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("GateSession", "id", sessionId));
        return gateSessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GateSessionResponse> getOpenSessions(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<GateSession> page = gateSessionRepository.findByStatusAndTenantId(GateSessionStatus.OPEN, tenantId, pageable);
        return toSessionPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GateSessionResponse> getAllSessions(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<GateSession> page = gateSessionRepository.findAllByTenantId(tenantId, pageable);
        return toSessionPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GateSessionResponse> getSessionsByVehicle(UUID vehicleId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<GateSession> page = gateSessionRepository.findByVehicleIdAndTenantId(vehicleId, tenantId, pageable);
        return toSessionPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GateAccessLogResponse> getAllAccessLogs(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<GateAccessLog> page = gateAccessLogRepository.findAllByTenantId(tenantId, pageable);
        return toLogPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GateAccessLogResponse> getAccessLogsBySession(UUID sessionId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<GateAccessLog> page = gateAccessLogRepository.findBySessionIdAndTenantId(sessionId, tenantId, pageable);
        return toLogPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GateAccessLogResponse> getAccessLogsByVehicle(UUID vehicleId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<GateAccessLog> page = gateAccessLogRepository.findByVehicleIdAndTenantId(vehicleId, tenantId, pageable);
        return toLogPagedResponse(page);
    }

    private void logEvent(UUID sessionId, UUID vehicleId, UUID residentId, GateEventType eventType,
                           String guardId, String details, boolean success) {
        UUID tenantId = TenantContext.requireTenantId();
        GateAccessLog accessLog = new GateAccessLog();
        accessLog.setTenantId(tenantId);
        accessLog.setSessionId(sessionId);
        accessLog.setVehicleId(vehicleId);
        accessLog.setResidentId(residentId);
        accessLog.setEventType(eventType);
        accessLog.setGuardId(guardId);
        accessLog.setDetails(details);
        accessLog.setSuccess(success);
        gateAccessLogRepository.save(accessLog);
    }

    private PagedResponse<GateSessionResponse> toSessionPagedResponse(Page<GateSession> page) {
        return PagedResponse.<GateSessionResponse>builder()
                .content(page.getContent().stream().map(gateSessionMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    private PagedResponse<GateAccessLogResponse> toLogPagedResponse(Page<GateAccessLog> page) {
        return PagedResponse.<GateAccessLogResponse>builder()
                .content(page.getContent().stream().map(gateAccessLogMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
