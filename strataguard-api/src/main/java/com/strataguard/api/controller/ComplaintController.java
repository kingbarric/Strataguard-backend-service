package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.ComplaintStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.governance.ComplaintService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Complaint and petition management")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ResidentRepository residentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Submit a complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdOrNull(jwt);
        ComplaintResponse response = complaintService.createComplaint(residentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Complaint submitted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get complaint by ID")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaint(@PathVariable UUID id) {
        ComplaintResponse response = complaintService.getComplaint(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all complaints")
    public ResponseEntity<ApiResponse<PagedResponse<ComplaintResponse>>> getAllComplaints(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ComplaintResponse> response = complaintService.getAllComplaints(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get complaints by estate")
    public ResponseEntity<ApiResponse<PagedResponse<ComplaintResponse>>> getComplaintsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ComplaintResponse> response = complaintService.getComplaintsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-complaints")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my complaints")
    public ResponseEntity<ApiResponse<PagedResponse<ComplaintResponse>>> getMyComplaints(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentId(jwt);
        PagedResponse<ComplaintResponse> response = complaintService.getMyComplaints(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get complaints by status")
    public ResponseEntity<ApiResponse<PagedResponse<ComplaintResponse>>> getComplaintsByStatus(
            @PathVariable ComplaintStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ComplaintResponse> response = complaintService.getComplaintsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Acknowledge a complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> acknowledgeComplaint(@PathVariable UUID id) {
        ComplaintResponse response = complaintService.acknowledgeComplaint(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint acknowledged"));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Assign a complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> assignComplaint(
            @PathVariable UUID id,
            @RequestParam String assignedTo,
            @RequestParam(required = false) String assignedToName) {
        ComplaintResponse response = complaintService.assignComplaint(id, assignedTo, assignedToName);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint assigned"));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Resolve a complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> resolveComplaint(
            @PathVariable UUID id,
            @RequestParam(required = false) String responseNotes) {
        ComplaintResponse response = complaintService.resolveComplaint(id, responseNotes);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint resolved"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'RESIDENT')")
    @Operation(summary = "Close a complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> closeComplaint(@PathVariable UUID id) {
        ComplaintResponse response = complaintService.closeComplaint(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint closed"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Reject a complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> rejectComplaint(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        ComplaintResponse response = complaintService.rejectComplaint(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint rejected"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Delete a complaint")
    public ResponseEntity<ApiResponse<Void>> deleteComplaint(@PathVariable UUID id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Complaint deleted"));
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
}
