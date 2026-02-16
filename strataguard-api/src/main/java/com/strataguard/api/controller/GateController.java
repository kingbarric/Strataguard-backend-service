package com.strataguard.api.controller;

import com.strataguard.core.dto.accesslog.GateAccessLogResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.gate.*;
import com.strataguard.service.gate.GateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gate")
@RequiredArgsConstructor
@Tag(name = "Gate Control", description = "Vehicle gate entry/exit management")
public class GateController {

    private final GateService gateService;

    @PostMapping("/entry")
    @PreAuthorize("hasRole('SECURITY_GUARD')")
    @Operation(summary = "Process vehicle entry by scanning QR sticker")
    public ResponseEntity<ApiResponse<GateEntryResponse>> processEntry(
            @Valid @RequestBody GateEntryRequest request) {
        GateEntryResponse response = gateService.processEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vehicle entry processed successfully"));
    }

    @PostMapping("/exit")
    @PreAuthorize("hasRole('SECURITY_GUARD')")
    @Operation(summary = "Process vehicle exit with QR sticker and exit pass token")
    public ResponseEntity<ApiResponse<GateExitResponse>> processExit(
            @Valid @RequestBody GateExitRequest request) {
        GateExitResponse response = gateService.processExit(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle exit processed successfully"));
    }

    @PostMapping("/exit/remote/{sessionId}")
    @PreAuthorize("hasRole('SECURITY_GUARD')")
    @Operation(summary = "Process vehicle exit after remote approval")
    public ResponseEntity<ApiResponse<GateExitResponse>> processRemoteApprovalExit(
            @PathVariable UUID sessionId) {
        GateExitResponse response = gateService.processRemoteApprovalExit(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle exit via remote approval processed successfully"));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get gate session by ID")
    public ResponseEntity<ApiResponse<GateSessionResponse>> getSession(@PathVariable UUID id) {
        GateSessionResponse response = gateService.getSession(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all gate sessions with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<GateSessionResponse>>> getAllSessions(
            @PageableDefault(size = 20, sort = "entryTime", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<GateSessionResponse> response = gateService.getAllSessions(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/open")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all currently open gate sessions")
    public ResponseEntity<ApiResponse<PagedResponse<GateSessionResponse>>> getOpenSessions(
            @PageableDefault(size = 20, sort = "entryTime", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<GateSessionResponse> response = gateService.getOpenSessions(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get gate sessions for a specific vehicle")
    public ResponseEntity<ApiResponse<PagedResponse<GateSessionResponse>>> getSessionsByVehicle(
            @PathVariable UUID vehicleId,
            @PageableDefault(size = 20, sort = "entryTime", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<GateSessionResponse> response = gateService.getSessionsByVehicle(vehicleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all gate access logs")
    public ResponseEntity<ApiResponse<PagedResponse<GateAccessLogResponse>>> getAllAccessLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<GateAccessLogResponse> response = gateService.getAllAccessLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'SECURITY_GUARD')")
    @Operation(summary = "Get access logs for a specific gate session")
    public ResponseEntity<ApiResponse<PagedResponse<GateAccessLogResponse>>> getAccessLogsBySession(
            @PathVariable UUID sessionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<GateAccessLogResponse> response = gateService.getAccessLogsBySession(sessionId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get access logs for a specific vehicle")
    public ResponseEntity<ApiResponse<PagedResponse<GateAccessLogResponse>>> getAccessLogsByVehicle(
            @PathVariable UUID vehicleId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<GateAccessLogResponse> response = gateService.getAccessLogsByVehicle(vehicleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
