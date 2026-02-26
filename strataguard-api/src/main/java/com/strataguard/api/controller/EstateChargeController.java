package com.strataguard.api.controller;

import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.service.billing.EstateChargeService;
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
@RequestMapping("/api/v1/estate-charges")
@RequiredArgsConstructor
@Tag(name = "Estate Charges", description = "Estate-wide charge management endpoints")
public class EstateChargeController {

    private final EstateChargeService estateChargeService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'charge.create')")
    @Operation(summary = "Create a new estate charge")
    public ResponseEntity<ApiResponse<EstateChargeResponse>> createEstateCharge(
            @Valid @RequestBody CreateEstateChargeRequest request) {
        EstateChargeResponse response = estateChargeService.createEstateCharge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Estate charge created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'charge.read')")
    @Operation(summary = "Get all estate charges with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<EstateChargeResponse>>> getAllEstateCharges(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<EstateChargeResponse> response = estateChargeService.getAllEstateCharges(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'charge.read')")
    @Operation(summary = "Get estate charge by ID")
    public ResponseEntity<ApiResponse<EstateChargeResponse>> getEstateCharge(@PathVariable UUID id) {
        EstateChargeResponse response = estateChargeService.getEstateCharge(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'charge.update')")
    @Operation(summary = "Update an estate charge")
    public ResponseEntity<ApiResponse<EstateChargeResponse>> updateEstateCharge(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEstateChargeRequest request) {
        EstateChargeResponse response = estateChargeService.updateEstateCharge(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Estate charge updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'charge.delete')")
    @Operation(summary = "Soft-delete an estate charge")
    public ResponseEntity<ApiResponse<Void>> deleteEstateCharge(@PathVariable UUID id) {
        estateChargeService.deleteEstateCharge(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Estate charge deleted successfully"));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasPermission(null, 'charge.read')")
    @Operation(summary = "Get estate charges by estate")
    public ResponseEntity<ApiResponse<PagedResponse<EstateChargeResponse>>> getEstateChargesByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<EstateChargeResponse> response = estateChargeService.getEstateChargesByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/exclusions")
    @PreAuthorize("hasPermission(null, 'charge.exclusion_manage')")
    @Operation(summary = "Add a tenancy exclusion to an estate charge")
    public ResponseEntity<ApiResponse<ExclusionResponse>> addExclusion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExclusionRequest request) {
        ExclusionResponse response = estateChargeService.addExclusion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Exclusion added successfully"));
    }

    @DeleteMapping("/{id}/exclusions/{exclusionId}")
    @PreAuthorize("hasPermission(null, 'charge.exclusion_manage')")
    @Operation(summary = "Remove a tenancy exclusion from an estate charge")
    public ResponseEntity<ApiResponse<Void>> removeExclusion(
            @PathVariable UUID id,
            @PathVariable UUID exclusionId) {
        estateChargeService.removeExclusion(id, exclusionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Exclusion removed successfully"));
    }

    @GetMapping("/{id}/exclusions")
    @PreAuthorize("hasPermission(null, 'charge.read')")
    @Operation(summary = "Get all exclusions for an estate charge")
    public ResponseEntity<ApiResponse<List<ExclusionResponse>>> getExclusions(@PathVariable UUID id) {
        List<ExclusionResponse> response = estateChargeService.getExclusions(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
