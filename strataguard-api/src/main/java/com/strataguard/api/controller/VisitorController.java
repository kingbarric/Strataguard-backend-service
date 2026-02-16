package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.visitor.*;
import com.strataguard.service.visitor.VisitorService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@Tag(name = "Visitors", description = "Visitor management and check-in/check-out endpoints")
public class VisitorController {

    private final VisitorService visitorService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Create a visitor invitation")
    public ResponseEntity<ApiResponse<VisitorResponse>> createVisitor(
            @Valid @RequestBody CreateVisitorRequest request) {
        VisitorResponse response = visitorService.createVisitor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Visitor invitation created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all visitors with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> getAllVisitors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<VisitorResponse> response = visitorService.getAllVisitors(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'SUPER_ADMIN', 'SECURITY_GUARD')")
    @Operation(summary = "Get visitor by ID")
    public ResponseEntity<ApiResponse<VisitorResponse>> getVisitor(@PathVariable UUID id) {
        VisitorResponse response = visitorService.getVisitor(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update a visitor")
    public ResponseEntity<ApiResponse<VisitorResponse>> updateVisitor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVisitorRequest request) {
        VisitorResponse response = visitorService.updateVisitor(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Visitor updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN')")
    @Operation(summary = "Delete a visitor")
    public ResponseEntity<ApiResponse<Void>> deleteVisitor(@PathVariable UUID id) {
        visitorService.deleteVisitor(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Visitor deleted successfully"));
    }

    @GetMapping("/my-visitors")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get visitors invited by the current resident")
    public ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> getMyVisitors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<VisitorResponse> response = visitorService.getMyVisitors(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'SECURITY_GUARD')")
    @Operation(summary = "Search visitors by name, phone, or email")
    public ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> searchVisitors(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<VisitorResponse> response = visitorService.searchVisitors(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/expected")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ESTATE_ADMIN')")
    @Operation(summary = "Get expected (pending) visitors")
    public ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> getExpectedVisitors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<VisitorResponse> response = visitorService.getExpectedVisitors(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/passes")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN')")
    @Operation(summary = "Get all passes for a visitor")
    public ResponseEntity<ApiResponse<List<VisitPassResponse>>> getVisitorPasses(@PathVariable UUID id) {
        List<VisitPassResponse> response = visitorService.getVisitorPasses(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/passes/regenerate")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Regenerate a visit pass for a visitor")
    public ResponseEntity<ApiResponse<VisitPassResponse>> regeneratePass(@PathVariable UUID id) {
        VisitPassResponse response = visitorService.regeneratePass(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Visit pass regenerated successfully"));
    }

    @PostMapping("/{id}/passes/{passId}/revoke")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN')")
    @Operation(summary = "Revoke a visit pass")
    public ResponseEntity<ApiResponse<Void>> revokePass(
            @PathVariable UUID id,
            @PathVariable UUID passId) {
        visitorService.revokePass(id, passId);
        return ResponseEntity.ok(ApiResponse.success(null, "Visit pass revoked successfully"));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('SECURITY_GUARD')")
    @Operation(summary = "Check in a visitor using QR token or verification code")
    public ResponseEntity<ApiResponse<VisitorCheckInResponse>> checkIn(
            @Valid @RequestBody VisitorCheckInRequest request) {
        VisitorCheckInResponse response = visitorService.checkIn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Visitor checked in successfully"));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('SECURITY_GUARD')")
    @Operation(summary = "Check out a visitor")
    public ResponseEntity<ApiResponse<Void>> checkOut(
            @Valid @RequestBody VisitorCheckOutRequest request) {
        visitorService.checkOut(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Visitor checked out successfully"));
    }
}
