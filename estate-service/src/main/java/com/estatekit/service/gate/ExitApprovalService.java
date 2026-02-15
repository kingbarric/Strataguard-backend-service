package com.estatekit.service.gate;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.approval.CreateExitApprovalRequest;
import com.estatekit.core.dto.approval.ExitApprovalResponse;
import com.estatekit.core.entity.ExitApprovalRequest;
import com.estatekit.core.entity.GateSession;
import com.estatekit.core.entity.Vehicle;
import com.estatekit.core.enums.ExitApprovalStatus;
import com.estatekit.core.enums.GateEventType;
import com.estatekit.core.enums.GateSessionStatus;
import com.estatekit.core.exception.GateAccessDeniedException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.ExitApprovalMapper;
import com.estatekit.infrastructure.repository.ExitApprovalRequestRepository;
import com.estatekit.infrastructure.repository.GateSessionRepository;
import com.estatekit.infrastructure.repository.VehicleRepository;
import com.estatekit.core.entity.GateAccessLog;
import com.estatekit.infrastructure.repository.GateAccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ExitApprovalService {

    private final ExitApprovalRequestRepository approvalRepository;
    private final GateSessionRepository gateSessionRepository;
    private final VehicleRepository vehicleRepository;
    private final GateAccessLogRepository gateAccessLogRepository;
    private final ExitApprovalMapper exitApprovalMapper;
    private final int expiryMinutes;

    public ExitApprovalService(ExitApprovalRequestRepository approvalRepository,
                               GateSessionRepository gateSessionRepository,
                               VehicleRepository vehicleRepository,
                               GateAccessLogRepository gateAccessLogRepository,
                               ExitApprovalMapper exitApprovalMapper,
                               @Value("${gate.exit-approval.expiry-minutes}") int expiryMinutes) {
        this.approvalRepository = approvalRepository;
        this.gateSessionRepository = gateSessionRepository;
        this.vehicleRepository = vehicleRepository;
        this.gateAccessLogRepository = gateAccessLogRepository;
        this.exitApprovalMapper = exitApprovalMapper;
        this.expiryMinutes = expiryMinutes;
    }

    public ExitApprovalResponse createApprovalRequest(CreateExitApprovalRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String guardId = SecurityContextHolder.getContext().getAuthentication().getName();

        GateSession session = gateSessionRepository.findByIdAndTenantId(request.getSessionId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("GateSession", "id", request.getSessionId()));

        if (session.getStatus() != GateSessionStatus.OPEN) {
            throw new GateAccessDeniedException("Session is not open. Current status: " + session.getStatus());
        }

        ExitApprovalRequest approval = new ExitApprovalRequest();
        approval.setTenantId(tenantId);
        approval.setSessionId(session.getId());
        approval.setVehicleId(session.getVehicleId());
        approval.setResidentId(session.getResidentId());
        approval.setGuardId(guardId);
        approval.setStatus(ExitApprovalStatus.PENDING);
        approval.setExpiresAt(Instant.now().plusSeconds(expiryMinutes * 60L));
        approval.setNote(request.getNote());

        ExitApprovalRequest saved = approvalRepository.save(approval);

        logEvent(session.getId(), session.getVehicleId(), session.getResidentId(),
                GateEventType.REMOTE_APPROVAL_REQUESTED, guardId, "Remote approval requested", true);

        log.info("Exit approval request created: {} for session: {} by guard: {}", saved.getId(), session.getId(), guardId);

        return enrichResponse(exitApprovalMapper.toResponse(saved), session.getVehicleId(), tenantId);
    }

    @Transactional(readOnly = true)
    public List<ExitApprovalResponse> getPendingApprovalsForResident(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();

        List<ExitApprovalRequest> approvals = approvalRepository
                .findByResidentIdAndStatusAndTenantId(residentId, ExitApprovalStatus.PENDING, tenantId);

        // Check for expired approvals and mark them
        Instant now = Instant.now();
        return approvals.stream()
                .map(a -> {
                    if (a.getExpiresAt().isBefore(now)) {
                        a.setStatus(ExitApprovalStatus.EXPIRED);
                        approvalRepository.save(a);
                        logEvent(a.getSessionId(), a.getVehicleId(), a.getResidentId(),
                                GateEventType.REMOTE_APPROVAL_EXPIRED, a.getGuardId(), "Approval request expired", true);
                        return null;
                    }
                    return enrichResponse(exitApprovalMapper.toResponse(a), a.getVehicleId(), tenantId);
                })
                .filter(r -> r != null)
                .toList();
    }

    public ExitApprovalResponse approveRequest(UUID approvalId) {
        UUID tenantId = TenantContext.requireTenantId();

        ExitApprovalRequest approval = approvalRepository.findByIdAndTenantId(approvalId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ExitApprovalRequest", "id", approvalId));

        if (approval.getStatus() != ExitApprovalStatus.PENDING) {
            throw new GateAccessDeniedException("Approval request is not pending. Current status: " + approval.getStatus());
        }

        // Check expiry
        if (approval.getExpiresAt().isBefore(Instant.now())) {
            approval.setStatus(ExitApprovalStatus.EXPIRED);
            approvalRepository.save(approval);
            logEvent(approval.getSessionId(), approval.getVehicleId(), approval.getResidentId(),
                    GateEventType.REMOTE_APPROVAL_EXPIRED, approval.getGuardId(), "Approval request expired", true);
            throw new GateAccessDeniedException("Approval request has expired");
        }

        approval.setStatus(ExitApprovalStatus.APPROVED);
        approval.setRespondedAt(Instant.now());
        ExitApprovalRequest updated = approvalRepository.save(approval);

        logEvent(approval.getSessionId(), approval.getVehicleId(), approval.getResidentId(),
                GateEventType.REMOTE_APPROVAL_APPROVED, approval.getGuardId(), "Resident approved exit", true);

        log.info("Exit approval approved: {} for session: {}", approvalId, approval.getSessionId());

        return enrichResponse(exitApprovalMapper.toResponse(updated), approval.getVehicleId(), tenantId);
    }

    public ExitApprovalResponse denyRequest(UUID approvalId) {
        UUID tenantId = TenantContext.requireTenantId();

        ExitApprovalRequest approval = approvalRepository.findByIdAndTenantId(approvalId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ExitApprovalRequest", "id", approvalId));

        if (approval.getStatus() != ExitApprovalStatus.PENDING) {
            throw new GateAccessDeniedException("Approval request is not pending. Current status: " + approval.getStatus());
        }

        approval.setStatus(ExitApprovalStatus.DENIED);
        approval.setRespondedAt(Instant.now());
        ExitApprovalRequest updated = approvalRepository.save(approval);

        logEvent(approval.getSessionId(), approval.getVehicleId(), approval.getResidentId(),
                GateEventType.REMOTE_APPROVAL_DENIED, approval.getGuardId(), "Resident denied exit", true);

        log.info("Exit approval denied: {} for session: {}", approvalId, approval.getSessionId());

        return enrichResponse(exitApprovalMapper.toResponse(updated), approval.getVehicleId(), tenantId);
    }

    @Transactional(readOnly = true)
    public ExitApprovalResponse getApprovalStatus(UUID approvalId) {
        UUID tenantId = TenantContext.requireTenantId();

        ExitApprovalRequest approval = approvalRepository.findByIdAndTenantId(approvalId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ExitApprovalRequest", "id", approvalId));

        // Check for expiry on read
        if (approval.getStatus() == ExitApprovalStatus.PENDING && approval.getExpiresAt().isBefore(Instant.now())) {
            approval.setStatus(ExitApprovalStatus.EXPIRED);
            approvalRepository.save(approval);
        }

        return enrichResponse(exitApprovalMapper.toResponse(approval), approval.getVehicleId(), tenantId);
    }

    private ExitApprovalResponse enrichResponse(ExitApprovalResponse response, UUID vehicleId, UUID tenantId) {
        vehicleRepository.findByIdAndTenantId(vehicleId, tenantId).ifPresent(vehicle -> {
            response.setPlateNumber(vehicle.getPlateNumber());
            response.setVehicleMake(vehicle.getMake());
            response.setVehicleModel(vehicle.getModel());
            response.setVehicleColor(vehicle.getColor());
        });
        return response;
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
}
