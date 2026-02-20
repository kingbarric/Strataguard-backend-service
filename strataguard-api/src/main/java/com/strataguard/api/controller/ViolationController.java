package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.enums.ViolationStatus;
import com.strataguard.service.governance.ViolationService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/violations")
@RequiredArgsConstructor
@Tag(name = "Violations", description = "Estate rule violation management")
public class ViolationController {

    private final ViolationService violationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'SECURITY_GUARD')")
    @Operation(summary = "Report a violation")
    public ResponseEntity<ApiResponse<ViolationResponse>> createViolation(
            @Valid @RequestBody CreateViolationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String reportedBy = jwt.getSubject();
        String reportedByName = jwt.getClaimAsString("preferred_username");
        ViolationResponse response = violationService.createViolation(request, reportedBy, reportedByName != null ? reportedByName : reportedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Violation reported successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get violation by ID")
    public ResponseEntity<ApiResponse<ViolationResponse>> getViolation(@PathVariable UUID id) {
        ViolationResponse response = violationService.getViolation(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all violations")
    public ResponseEntity<ApiResponse<PagedResponse<ViolationResponse>>> getAllViolations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ViolationResponse> response = violationService.getAllViolations(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get violations by estate")
    public ResponseEntity<ApiResponse<PagedResponse<ViolationResponse>>> getViolationsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ViolationResponse> response = violationService.getViolationsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unit/{unitId}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get violations by unit")
    public ResponseEntity<ApiResponse<PagedResponse<ViolationResponse>>> getViolationsByUnit(
            @PathVariable UUID unitId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ViolationResponse> response = violationService.getViolationsByUnit(unitId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get violations by status")
    public ResponseEntity<ApiResponse<PagedResponse<ViolationResponse>>> getViolationsByStatus(
            @PathVariable ViolationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ViolationResponse> response = violationService.getViolationsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Confirm a violation")
    public ResponseEntity<ApiResponse<ViolationResponse>> confirmViolation(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal fineAmount) {
        ViolationResponse response = violationService.confirmViolation(id, fineAmount);
        return ResponseEntity.ok(ApiResponse.success(response, "Violation confirmed"));
    }

    @PostMapping("/{id}/fine")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Issue fine for violation")
    public ResponseEntity<ApiResponse<ViolationResponse>> issueFine(@PathVariable UUID id) {
        ViolationResponse response = violationService.issueFinance(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Fine issued"));
    }

    @PostMapping("/{id}/appeal")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Appeal a violation")
    public ResponseEntity<ApiResponse<ViolationResponse>> appealViolation(
            @PathVariable UUID id,
            @RequestParam String reason) {
        ViolationResponse response = violationService.appealViolation(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Appeal submitted"));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Dismiss a violation")
    public ResponseEntity<ApiResponse<ViolationResponse>> dismissViolation(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {
        ViolationResponse response = violationService.dismissViolation(id, notes);
        return ResponseEntity.ok(ApiResponse.success(response, "Violation dismissed"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Close a violation")
    public ResponseEntity<ApiResponse<ViolationResponse>> closeViolation(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {
        ViolationResponse response = violationService.closeViolation(id, notes);
        return ResponseEntity.ok(ApiResponse.success(response, "Violation closed"));
    }
}
