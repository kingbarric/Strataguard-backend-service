package com.strataguard.api.controller;

import com.strataguard.core.dto.billing.CreateLevyTypeRequest;
import com.strataguard.core.dto.billing.LevyTypeResponse;
import com.strataguard.core.dto.billing.UpdateLevyTypeRequest;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.service.billing.LevyTypeService;
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
@RequestMapping("/api/v1/levy-types")
@RequiredArgsConstructor
@Tag(name = "Levy Types", description = "Levy type management endpoints")
public class LevyTypeController {

    private final LevyTypeService levyTypeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new levy type")
    public ResponseEntity<ApiResponse<LevyTypeResponse>> createLevyType(
            @Valid @RequestBody CreateLevyTypeRequest request) {
        LevyTypeResponse response = levyTypeService.createLevyType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Levy type created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all levy types with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<LevyTypeResponse>>> getAllLevyTypes(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<LevyTypeResponse> response = levyTypeService.getAllLevyTypes(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get levy type by ID")
    public ResponseEntity<ApiResponse<LevyTypeResponse>> getLevyType(@PathVariable UUID id) {
        LevyTypeResponse response = levyTypeService.getLevyType(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update a levy type")
    public ResponseEntity<ApiResponse<LevyTypeResponse>> updateLevyType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLevyTypeRequest request) {
        LevyTypeResponse response = levyTypeService.updateLevyType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Levy type updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Soft-delete a levy type")
    public ResponseEntity<ApiResponse<Void>> deleteLevyType(@PathVariable UUID id) {
        levyTypeService.deleteLevyType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Levy type deleted successfully"));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get levy types by estate")
    public ResponseEntity<ApiResponse<PagedResponse<LevyTypeResponse>>> getLevyTypesByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<LevyTypeResponse> response = levyTypeService.getLevyTypesByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
