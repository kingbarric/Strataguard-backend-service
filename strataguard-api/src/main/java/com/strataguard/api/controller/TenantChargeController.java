package com.strataguard.api.controller;

import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.service.billing.TenantChargeService;
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
@RequestMapping("/api/v1/tenant-charges")
@RequiredArgsConstructor
@Tag(name = "Tenant Charges", description = "Tenancy-specific charge management endpoints")
public class TenantChargeController {

    private final TenantChargeService tenantChargeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new tenant charge")
    public ResponseEntity<ApiResponse<TenantChargeResponse>> createTenantCharge(
            @Valid @RequestBody CreateTenantChargeRequest request) {
        TenantChargeResponse response = tenantChargeService.createTenantCharge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tenant charge created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all tenant charges with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<TenantChargeResponse>>> getAllTenantCharges(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenantChargeResponse> response = tenantChargeService.getAllTenantCharges(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get tenant charge by ID")
    public ResponseEntity<ApiResponse<TenantChargeResponse>> getTenantCharge(@PathVariable UUID id) {
        TenantChargeResponse response = tenantChargeService.getTenantCharge(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update a tenant charge")
    public ResponseEntity<ApiResponse<TenantChargeResponse>> updateTenantCharge(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantChargeRequest request) {
        TenantChargeResponse response = tenantChargeService.updateTenantCharge(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant charge updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Soft-delete a tenant charge")
    public ResponseEntity<ApiResponse<Void>> deleteTenantCharge(@PathVariable UUID id) {
        tenantChargeService.deleteTenantCharge(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant charge deleted successfully"));
    }

    @GetMapping("/tenancy/{tenancyId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get tenant charges by tenancy")
    public ResponseEntity<ApiResponse<PagedResponse<TenantChargeResponse>>> getTenantChargesByTenancy(
            @PathVariable UUID tenancyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenantChargeResponse> response = tenantChargeService.getTenantChargesByTenancy(tenancyId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get tenant charges by estate")
    public ResponseEntity<ApiResponse<PagedResponse<TenantChargeResponse>>> getTenantChargesByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenantChargeResponse> response = tenantChargeService.getTenantChargesByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
