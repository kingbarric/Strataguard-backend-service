package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.tenancy.CreateTenancyRequest;
import com.strataguard.core.dto.tenancy.TenancyResponse;
import com.strataguard.core.dto.tenancy.UpdateTenancyRequest;
import com.strataguard.service.resident.TenancyService;
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
@RequestMapping("/api/v1/tenancies")
@RequiredArgsConstructor
@Tag(name = "Tenancies", description = "Tenancy management endpoints")
public class TenancyController {

    private final TenancyService tenancyService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'tenancy.create')")
    @Operation(summary = "Create a new tenancy (move-in)")
    public ResponseEntity<ApiResponse<TenancyResponse>> createTenancy(
            @Valid @RequestBody CreateTenancyRequest request) {
        TenancyResponse response = tenancyService.createTenancy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tenancy created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'tenancy.read')")
    @Operation(summary = "Get all tenancies")
    public ResponseEntity<ApiResponse<PagedResponse<TenancyResponse>>> getAllTenancies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenancyResponse> response = tenancyService.getAllTenancies(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'tenancy.read')")
    @Operation(summary = "Get tenancy by ID")
    public ResponseEntity<ApiResponse<TenancyResponse>> getTenancy(@PathVariable UUID id) {
        TenancyResponse response = tenancyService.getTenancy(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasPermission(null, 'tenancy.read')")
    @Operation(summary = "Get all tenancies for an estate")
    public ResponseEntity<ApiResponse<PagedResponse<TenancyResponse>>> getTenanciesByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenancyResponse> response = tenancyService.getTenanciesByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasPermission(null, 'tenancy.read')")
    @Operation(summary = "Get all tenancies for a resident")
    public ResponseEntity<ApiResponse<PagedResponse<TenancyResponse>>> getTenanciesByResident(
            @PathVariable UUID residentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenancyResponse> response = tenancyService.getTenanciesByResident(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unit/{unitId}")
    @PreAuthorize("hasPermission(null, 'tenancy.read')")
    @Operation(summary = "Get all tenancies for a unit")
    public ResponseEntity<ApiResponse<PagedResponse<TenancyResponse>>> getTenanciesByUnit(
            @PathVariable UUID unitId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TenancyResponse> response = tenancyService.getTenanciesByUnit(unitId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'tenancy.update')")
    @Operation(summary = "Update a tenancy")
    public ResponseEntity<ApiResponse<TenancyResponse>> updateTenancy(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenancyRequest request) {
        TenancyResponse response = tenancyService.updateTenancy(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenancy updated successfully"));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasPermission(null, 'tenancy.terminate')")
    @Operation(summary = "Terminate a tenancy (move-out)")
    public ResponseEntity<ApiResponse<TenancyResponse>> terminateTenancy(@PathVariable UUID id) {
        TenancyResponse response = tenancyService.terminateTenancy(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenancy terminated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'tenancy.terminate')")
    @Operation(summary = "Soft-delete a tenancy")
    public ResponseEntity<ApiResponse<Void>> deleteTenancy(@PathVariable UUID id) {
        tenancyService.deleteTenancy(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenancy deleted successfully"));
    }
}
