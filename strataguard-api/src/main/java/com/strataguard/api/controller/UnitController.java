package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.unit.CreateUnitRequest;
import com.strataguard.core.dto.unit.UnitResponse;
import com.strataguard.core.dto.unit.UpdateUnitRequest;
import com.strataguard.service.estate.UnitService;
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
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
@Tag(name = "Units", description = "Unit management endpoints")
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Create a new unit")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(
            @Valid @RequestBody CreateUnitRequest request) {
        UnitResponse response = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Unit created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get unit by ID")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnit(@PathVariable UUID id) {
        UnitResponse response = unitService.getUnit(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @Operation(summary = "Get all units for an estate")
    public ResponseEntity<ApiResponse<PagedResponse<UnitResponse>>> getUnitsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "unitNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<UnitResponse> response = unitService.getUnitsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Update a unit")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUnitRequest request) {
        UnitResponse response = unitService.updateUnit(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Soft-delete a unit")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable UUID id) {
        unitService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Unit deleted successfully"));
    }

    @GetMapping("/estate/{estateId}/count")
    @Operation(summary = "Count units in an estate")
    public ResponseEntity<ApiResponse<Long>> countUnits(@PathVariable UUID estateId) {
        long count = unitService.countUnitsByEstate(estateId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
