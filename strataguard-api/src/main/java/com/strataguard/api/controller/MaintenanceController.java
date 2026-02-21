package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.maintenance.*;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.MaintenanceStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.maintenance.MaintenanceService;
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
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Maintenance request management endpoints")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final ResidentRepository residentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Create a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> createRequest(
            @Valid @RequestBody CreateMaintenanceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdOrNull(jwt);
        MaintenanceResponse response = maintenanceService.createRequest(residentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Maintenance request created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Update a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> updateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceRequest request) {
        MaintenanceResponse response = maintenanceService.updateRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Maintenance request updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get maintenance request by ID")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getRequest(@PathVariable UUID id) {
        MaintenanceResponse response = maintenanceService.getRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all maintenance requests")
    public ResponseEntity<ApiResponse<PagedResponse<MaintenanceResponse>>> getAllRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<MaintenanceResponse> response = maintenanceService.getAllRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my maintenance requests")
    public ResponseEntity<ApiResponse<PagedResponse<MaintenanceResponse>>> getMyRequests(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentId(jwt);
        PagedResponse<MaintenanceResponse> response = maintenanceService.getMyRequests(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get maintenance requests by estate")
    public ResponseEntity<ApiResponse<PagedResponse<MaintenanceResponse>>> getRequestsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<MaintenanceResponse> response = maintenanceService.getRequestsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get maintenance requests by status")
    public ResponseEntity<ApiResponse<PagedResponse<MaintenanceResponse>>> getRequestsByStatus(
            @PathVariable MaintenanceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<MaintenanceResponse> response = maintenanceService.getRequestsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Search maintenance requests")
    public ResponseEntity<ApiResponse<PagedResponse<MaintenanceResponse>>> searchRequests(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<MaintenanceResponse> response = maintenanceService.searchRequests(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Assign a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> assignRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AssignMaintenanceRequest request) {
        MaintenanceResponse response = maintenanceService.assignRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Request assigned successfully"));
    }

    @PostMapping("/{id}/cost-estimate")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Submit cost estimate for a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> submitCostEstimate(
            @PathVariable UUID id,
            @Valid @RequestBody CostEstimateRequest request) {
        MaintenanceResponse response = maintenanceService.submitCostEstimate(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cost estimate submitted"));
    }

    @PostMapping("/{id}/approve-cost")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'RESIDENT')")
    @Operation(summary = "Approve cost estimate")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> approveCostEstimate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String approvedBy = jwt.getClaimAsString("preferred_username");
        MaintenanceResponse response = maintenanceService.approveCostEstimate(id, approvedBy != null ? approvedBy : jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(response, "Cost estimate approved"));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Resolve a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> resolveRequest(
            @PathVariable UUID id,
            @RequestParam(required = false) String resolutionNotes,
            @RequestParam(required = false) BigDecimal actualCost) {
        MaintenanceResponse response = maintenanceService.resolveRequest(id, resolutionNotes, actualCost);
        return ResponseEntity.ok(ApiResponse.success(response, "Request resolved"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'RESIDENT')")
    @Operation(summary = "Close a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> closeRequest(@PathVariable UUID id) {
        MaintenanceResponse response = maintenanceService.closeRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Request closed"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'RESIDENT')")
    @Operation(summary = "Cancel a maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> cancelRequest(@PathVariable UUID id) {
        MaintenanceResponse response = maintenanceService.cancelRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Request cancelled"));
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Rate a resolved maintenance request")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> rateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RateMaintenanceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentId(jwt);
        MaintenanceResponse response = maintenanceService.rateRequest(id, residentId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Request rated successfully"));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Add comment to a maintenance request")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID authorId = getResidentIdOrNull(jwt);
        String authorName = jwt.getClaimAsString("preferred_username");
        if (authorName == null) authorName = jwt.getSubject();
        String authorRole = determineRole(jwt);
        CommentResponse response = maintenanceService.addComment(id,
                authorId != null ? authorId : UUID.fromString(jwt.getSubject()),
                authorName, authorRole, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Comment added"));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get comments for a maintenance request")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getComments(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        boolean includeInternal = hasAdminRole(jwt);
        PagedResponse<CommentResponse> response = maintenanceService.getComments(id, includeInternal, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get maintenance dashboard")
    public ResponseEntity<ApiResponse<MaintenanceDashboardResponse>> getDashboard() {
        MaintenanceDashboardResponse response = maintenanceService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getResidentId(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId));
        return resident.getId();
    }

    private UUID getResidentIdOrNull(Jwt jwt) {
        try {
            return getResidentId(jwt);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasAdminRole(Jwt jwt) {
        var roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles == null) return false;
        return roles.contains("ESTATE_ADMIN") || roles.contains("SUPER_ADMIN") || roles.contains("FACILITY_MANAGER");
    }

    private String determineRole(Jwt jwt) {
        var roles = jwt.getClaimAsStringList("realm_access.roles");
        if (roles != null) {
            if (roles.contains("ESTATE_ADMIN") || roles.contains("SUPER_ADMIN")) return "ADMIN";
            if (roles.contains("FACILITY_MANAGER")) return "FACILITY_MANAGER";
        }
        return "RESIDENT";
    }
}
