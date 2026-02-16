package com.estatekit.service.visitor;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.visitor.*;
import com.estatekit.core.entity.*;
import com.estatekit.core.enums.*;
import com.estatekit.core.exception.BlacklistedException;
import com.estatekit.core.exception.GateAccessDeniedException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.*;
import com.estatekit.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final VisitPassRepository visitPassRepository;
    private final BlacklistRepository blacklistRepository;
    private final ResidentRepository residentRepository;
    private final GateAccessLogRepository gateAccessLogRepository;
    private final VisitorMapper visitorMapper;
    private final VisitPassMapper visitPassMapper;
    private final VisitorPassService visitorPassService;

    public VisitorResponse createVisitor(CreateVisitorRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Look up the resident by the current user's Keycloak ID
        Resident resident = residentRepository.findByUserIdAndTenantId(currentUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", currentUserId));

        // Normalize plate number if provided
        String normalizedPlate = null;
        if (request.getVehiclePlateNumber() != null && !request.getVehiclePlateNumber().isBlank()) {
            normalizedPlate = PlateNumberUtils.normalize(request.getVehiclePlateNumber());
        }

        Visitor visitor = visitorMapper.toEntity(request);
        visitor.setTenantId(tenantId);
        visitor.setInvitedBy(resident.getId());
        if (normalizedPlate != null) {
            visitor.setVehiclePlateNumber(normalizedPlate);
        }

        Visitor saved = visitorRepository.save(visitor);

        // Generate visit pass
        VisitPass pass = generatePass(saved, request, tenantId);

        log.info("Created visitor: {} with pass: {} for tenant: {}", saved.getId(), pass.getId(), tenantId);

        VisitorResponse response = visitorMapper.toResponse(saved);
        response.setInvitedByName(resident.getFirstName() + " " + resident.getLastName());
        return response;
    }

    @Transactional(readOnly = true)
    public VisitorResponse getVisitor(UUID visitorId) {
        UUID tenantId = TenantContext.requireTenantId();
        Visitor visitor = visitorRepository.findByIdAndTenantId(visitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));

        VisitorResponse response = visitorMapper.toResponse(visitor);
        enrichWithInviterName(response, visitor.getInvitedBy(), tenantId);
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<VisitorResponse> getAllVisitors(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Visitor> page = visitorRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page, tenantId);
    }

    public VisitorResponse updateVisitor(UUID visitorId, UpdateVisitorRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Visitor visitor = visitorRepository.findByIdAndTenantId(visitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));

        if (request.getVehiclePlateNumber() != null) {
            request.setVehiclePlateNumber(PlateNumberUtils.normalize(request.getVehiclePlateNumber()));
        }

        visitorMapper.updateEntity(request, visitor);
        Visitor updated = visitorRepository.save(visitor);
        log.info("Updated visitor: {} for tenant: {}", visitorId, tenantId);

        VisitorResponse response = visitorMapper.toResponse(updated);
        enrichWithInviterName(response, updated.getInvitedBy(), tenantId);
        return response;
    }

    public void deleteVisitor(UUID visitorId) {
        UUID tenantId = TenantContext.requireTenantId();
        Visitor visitor = visitorRepository.findByIdAndTenantId(visitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));

        visitor.setDeleted(true);
        visitorRepository.save(visitor);

        // Revoke all active passes
        List<VisitPass> passes = visitPassRepository.findByVisitorIdAndTenantId(visitorId, tenantId);
        passes.stream()
                .filter(p -> p.getStatus() == VisitPassStatus.ACTIVE)
                .forEach(p -> {
                    p.setStatus(VisitPassStatus.REVOKED);
                    visitPassRepository.save(p);
                });

        log.info("Soft-deleted visitor: {} for tenant: {}", visitorId, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VisitorResponse> getMyVisitors(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Resident resident = residentRepository.findByUserIdAndTenantId(currentUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", currentUserId));

        Page<Visitor> page = visitorRepository.findByInvitedByAndTenantId(resident.getId(), tenantId, pageable);
        return toPagedResponse(page, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VisitorResponse> searchVisitors(String query, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Visitor> page = visitorRepository.search(query, tenantId, pageable);
        return toPagedResponse(page, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VisitorResponse> getExpectedVisitors(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Visitor> page = visitorRepository.findExpectedVisitors(tenantId, pageable);
        return toPagedResponse(page, tenantId);
    }

    @Transactional(readOnly = true)
    public List<VisitPassResponse> getVisitorPasses(UUID visitorId) {
        UUID tenantId = TenantContext.requireTenantId();

        visitorRepository.findByIdAndTenantId(visitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));

        List<VisitPass> passes = visitPassRepository.findByVisitorIdAndTenantId(visitorId, tenantId);
        return passes.stream().map(visitPassMapper::toResponse).toList();
    }

    public VisitPassResponse regeneratePass(UUID visitorId) {
        UUID tenantId = TenantContext.requireTenantId();

        Visitor visitor = visitorRepository.findByIdAndTenantId(visitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));

        // Revoke existing active pass
        visitPassRepository.findActiveByVisitorIdAndTenantId(visitorId, tenantId)
                .ifPresent(pass -> {
                    pass.setStatus(VisitPassStatus.REVOKED);
                    visitPassRepository.save(pass);
                });

        // Get the last pass to use its config for regeneration
        List<VisitPass> existingPasses = visitPassRepository.findByVisitorIdAndTenantId(visitorId, tenantId);
        if (existingPasses.isEmpty()) {
            throw new ResourceNotFoundException("No existing pass found for visitor: " + visitorId);
        }

        VisitPass lastPass = existingPasses.get(existingPasses.size() - 1);

        // Create new pass with same config
        String passCode = UUID.randomUUID().toString();
        String verificationCode = VerificationCodeUtils.generate();
        String token = visitorPassService.generateToken(passCode, visitorId, tenantId, lastPass.getValidTo().toEpochMilli());
        String qrData = QrCodeUtils.generateBase64Png(token);

        VisitPass newPass = new VisitPass();
        newPass.setTenantId(tenantId);
        newPass.setVisitorId(visitorId);
        newPass.setPassCode(passCode);
        newPass.setQrData(qrData);
        newPass.setToken(token);
        newPass.setPassType(lastPass.getPassType());
        newPass.setValidFrom(lastPass.getValidFrom());
        newPass.setValidTo(lastPass.getValidTo());
        newPass.setMaxEntries(lastPass.getMaxEntries());
        newPass.setVerificationCode(verificationCode);
        newPass.setRecurringDays(lastPass.getRecurringDays());
        newPass.setRecurringStartTime(lastPass.getRecurringStartTime());
        newPass.setRecurringEndTime(lastPass.getRecurringEndTime());

        VisitPass saved = visitPassRepository.save(newPass);
        log.info("Regenerated pass: {} for visitor: {} tenant: {}", saved.getId(), visitorId, tenantId);
        return visitPassMapper.toResponse(saved);
    }

    public void revokePass(UUID visitorId, UUID passId) {
        UUID tenantId = TenantContext.requireTenantId();

        visitorRepository.findByIdAndTenantId(visitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", visitorId));

        VisitPass pass = visitPassRepository.findByIdAndTenantId(passId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("VisitPass", "id", passId));

        if (!pass.getVisitorId().equals(visitorId)) {
            throw new ResourceNotFoundException("VisitPass", "id", passId);
        }

        pass.setStatus(VisitPassStatus.REVOKED);
        visitPassRepository.save(pass);
        log.info("Revoked pass: {} for visitor: {} tenant: {}", passId, visitorId, tenantId);
    }

    public VisitorCheckInResponse checkIn(VisitorCheckInRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String guardId = SecurityContextHolder.getContext().getAuthentication().getName();

        if ((request.getToken() == null || request.getToken().isBlank())
                && (request.getVerificationCode() == null || request.getVerificationCode().isBlank())) {
            throw new GateAccessDeniedException("Either token or verification code is required");
        }

        // 1. Resolve pass
        VisitPass pass = resolvePass(request, tenantId);

        // 2. Validate pass status
        if (pass.getStatus() != VisitPassStatus.ACTIVE) {
            logVisitorEvent(null, pass.getVisitorId(), GateEventType.VISITOR_PASS_FAILED, guardId,
                    "Pass status: " + pass.getStatus(), false);
            throw new GateAccessDeniedException("Visit pass is not active. Status: " + pass.getStatus());
        }

        // Load visitor
        Visitor visitor = visitorRepository.findByIdAndTenantId(pass.getVisitorId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", pass.getVisitorId()));

        // 3. Check blacklist (phone + plate)
        checkBlacklist(visitor, guardId, tenantId);

        // 4. Check time validity
        Instant now = Instant.now();
        if (now.isBefore(pass.getValidFrom()) || now.isAfter(pass.getValidTo())) {
            logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_PASS_FAILED, guardId,
                    "Pass outside valid time window", false);
            throw new GateAccessDeniedException("Visit pass is not valid at this time");
        }

        // 5. For RECURRING: check day-of-week and time
        if (pass.getPassType() == VisitPassType.RECURRING) {
            validateRecurringSchedule(pass, visitor.getId(), guardId);
        }

        // 6. For SINGLE_USE/MULTI_USE: check entry count
        if (pass.getPassType() == VisitPassType.SINGLE_USE || pass.getPassType() == VisitPassType.MULTI_USE) {
            if (pass.getMaxEntries() != null && pass.getUsedEntries() >= pass.getMaxEntries()) {
                logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_PASS_FAILED, guardId,
                        "Max entries reached: " + pass.getUsedEntries() + "/" + pass.getMaxEntries(), false);
                throw new GateAccessDeniedException("Visit pass has reached maximum entries");
            }
        }

        // 7. Increment usedEntries; if SINGLE_USE → pass status = USED
        pass.setUsedEntries(pass.getUsedEntries() + 1);
        if (pass.getPassType() == VisitPassType.SINGLE_USE) {
            pass.setStatus(VisitPassStatus.USED);
        }
        visitPassRepository.save(pass);

        // 8. Visitor status → CHECKED_IN
        visitor.setStatus(VisitorStatus.CHECKED_IN);
        visitorRepository.save(visitor);

        // Log events
        logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_PASS_VALIDATED, guardId,
                "Pass validated successfully", true);
        logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_CHECK_IN, guardId,
                request.getNote() != null ? request.getNote() : "Visitor checked in", true);

        // Enrich response with host details
        Resident host = residentRepository.findByIdAndTenantId(visitor.getInvitedBy(), tenantId).orElse(null);

        log.info("Visitor checked in: {} pass: {} guard: {} tenant: {}", visitor.getId(), pass.getId(), guardId, tenantId);

        return VisitorCheckInResponse.builder()
                .visitorId(visitor.getId())
                .visitorName(visitor.getName())
                .visitorPhone(visitor.getPhone())
                .visitorType(visitor.getVisitorType())
                .vehiclePlateNumber(visitor.getVehiclePlateNumber())
                .purpose(visitor.getPurpose())
                .hostResidentId(visitor.getInvitedBy())
                .hostName(host != null ? host.getFirstName() + " " + host.getLastName() : null)
                .passType(pass.getPassType())
                .usedEntries(pass.getUsedEntries())
                .maxEntries(pass.getMaxEntries())
                .validTo(pass.getValidTo())
                .checkInTime(Instant.now())
                .build();
    }

    public void checkOut(VisitorCheckOutRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String guardId = SecurityContextHolder.getContext().getAuthentication().getName();

        Visitor visitor = visitorRepository.findByIdAndTenantId(request.getVisitorId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", "id", request.getVisitorId()));

        if (visitor.getStatus() != VisitorStatus.CHECKED_IN) {
            throw new GateAccessDeniedException("Visitor is not checked in. Current status: " + visitor.getStatus());
        }

        visitor.setStatus(VisitorStatus.CHECKED_OUT);
        visitorRepository.save(visitor);

        logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_CHECK_OUT, guardId,
                request.getNote() != null ? request.getNote() : "Visitor checked out", true);

        log.info("Visitor checked out: {} guard: {} tenant: {}", visitor.getId(), guardId, tenantId);
    }

    // --- Private helpers ---

    private VisitPass generatePass(Visitor visitor, CreateVisitorRequest request, UUID tenantId) {
        String passCode = UUID.randomUUID().toString();
        String verificationCode = VerificationCodeUtils.generate();
        String token = visitorPassService.generateToken(passCode, visitor.getId(), tenantId, request.getValidTo().toEpochMilli());
        String qrData = QrCodeUtils.generateBase64Png(token);

        VisitPass pass = new VisitPass();
        pass.setTenantId(tenantId);
        pass.setVisitorId(visitor.getId());
        pass.setPassCode(passCode);
        pass.setQrData(qrData);
        pass.setToken(token);
        pass.setPassType(request.getPassType());
        pass.setValidFrom(request.getValidFrom());
        pass.setValidTo(request.getValidTo());
        pass.setMaxEntries(request.getMaxEntries());
        pass.setVerificationCode(verificationCode);
        pass.setRecurringDays(request.getRecurringDays());
        pass.setRecurringStartTime(request.getRecurringStartTime());
        pass.setRecurringEndTime(request.getRecurringEndTime());

        return visitPassRepository.save(pass);
    }

    private VisitPass resolvePass(VisitorCheckInRequest request, UUID tenantId) {
        if (request.getToken() != null && !request.getToken().isBlank()) {
            // Validate HMAC signature first
            if (!visitorPassService.validateToken(request.getToken(), tenantId)) {
                throw new GateAccessDeniedException("Invalid or tampered visit pass token");
            }
            // Find pass by token
            return visitPassRepository.findByToken(request.getToken())
                    .orElseThrow(() -> new ResourceNotFoundException("VisitPass", "token", "***"));
        }

        // Resolve by verification code
        return visitPassRepository.findByVerificationCodeAndTenantId(request.getVerificationCode(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("VisitPass", "verificationCode", "***"));
    }

    private void checkBlacklist(Visitor visitor, String guardId, UUID tenantId) {
        if (visitor.getPhone() != null && !visitor.getPhone().isBlank()) {
            if (blacklistRepository.isPhoneBlacklisted(visitor.getPhone(), tenantId)) {
                logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_DENIED_BLACKLIST, guardId,
                        "Phone blacklisted: " + visitor.getPhone(), false);
                throw new BlacklistedException("Visitor is blacklisted (phone: " + visitor.getPhone() + ")");
            }
        }

        if (visitor.getVehiclePlateNumber() != null && !visitor.getVehiclePlateNumber().isBlank()) {
            String normalizedPlate = PlateNumberUtils.normalize(visitor.getVehiclePlateNumber());
            if (blacklistRepository.isPlateBlacklisted(normalizedPlate, tenantId)) {
                logVisitorEvent(null, visitor.getId(), GateEventType.VISITOR_DENIED_BLACKLIST, guardId,
                        "Plate blacklisted: " + normalizedPlate, false);
                throw new BlacklistedException("Visitor vehicle is blacklisted (plate: " + normalizedPlate + ")");
            }
        }
    }

    private void validateRecurringSchedule(VisitPass pass, UUID visitorId, String guardId) {
        LocalDateTime now = LocalDateTime.now();

        // Check day of week
        if (pass.getRecurringDays() != null && !pass.getRecurringDays().isBlank()) {
            String currentDay = now.getDayOfWeek().name().substring(0, 3);
            String[] allowedDays = pass.getRecurringDays().split(",");
            boolean dayAllowed = false;
            for (String day : allowedDays) {
                if (day.trim().equalsIgnoreCase(currentDay)) {
                    dayAllowed = true;
                    break;
                }
            }
            if (!dayAllowed) {
                logVisitorEvent(null, visitorId, GateEventType.VISITOR_PASS_FAILED, guardId,
                        "Not allowed on " + currentDay + ". Allowed: " + pass.getRecurringDays(), false);
                throw new GateAccessDeniedException("Visit pass is not valid on " + now.getDayOfWeek());
            }
        }

        // Check time of day
        if (pass.getRecurringStartTime() != null && pass.getRecurringEndTime() != null) {
            LocalTime currentTime = now.toLocalTime();
            if (currentTime.isBefore(pass.getRecurringStartTime()) || currentTime.isAfter(pass.getRecurringEndTime())) {
                logVisitorEvent(null, visitorId, GateEventType.VISITOR_PASS_FAILED, guardId,
                        "Outside allowed time: " + pass.getRecurringStartTime() + "-" + pass.getRecurringEndTime(), false);
                throw new GateAccessDeniedException("Visit pass is not valid at this time of day");
            }
        }
    }

    private void logVisitorEvent(UUID sessionId, UUID visitorId, GateEventType eventType,
                                  String guardId, String details, boolean success) {
        UUID tenantId = TenantContext.requireTenantId();
        GateAccessLog accessLog = new GateAccessLog();
        accessLog.setTenantId(tenantId);
        accessLog.setSessionId(sessionId);
        accessLog.setVisitorId(visitorId);
        accessLog.setEventType(eventType);
        accessLog.setGuardId(guardId);
        accessLog.setDetails(details);
        accessLog.setSuccess(success);
        gateAccessLogRepository.save(accessLog);
    }

    private void enrichWithInviterName(VisitorResponse response, UUID invitedBy, UUID tenantId) {
        residentRepository.findByIdAndTenantId(invitedBy, tenantId)
                .ifPresent(r -> response.setInvitedByName(r.getFirstName() + " " + r.getLastName()));
    }

    private PagedResponse<VisitorResponse> toPagedResponse(Page<Visitor> page, UUID tenantId) {
        List<VisitorResponse> content = page.getContent().stream()
                .map(v -> {
                    VisitorResponse response = visitorMapper.toResponse(v);
                    enrichWithInviterName(response, v.getInvitedBy(), tenantId);
                    return response;
                })
                .toList();

        return PagedResponse.<VisitorResponse>builder()
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
