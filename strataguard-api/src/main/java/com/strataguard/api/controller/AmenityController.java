package com.strataguard.api.controller;

import com.strataguard.core.dto.amenity.*;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.enums.AmenityStatus;
import com.strataguard.service.amenity.AmenityService;
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
@RequestMapping("/api/v1/amenities")
@RequiredArgsConstructor
@Tag(name = "Amenities", description = "Amenity management endpoints")
public class AmenityController {

    private final AmenityService amenityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new amenity")
    public ResponseEntity<ApiResponse<AmenityResponse>> createAmenity(
            @Valid @RequestBody CreateAmenityRequest request) {
        AmenityResponse response = amenityService.createAmenity(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Amenity created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an amenity")
    public ResponseEntity<ApiResponse<AmenityResponse>> updateAmenity(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAmenityRequest request) {
        AmenityResponse response = amenityService.updateAmenity(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Amenity updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'RESIDENT', 'FACILITY_MANAGER')")
    @Operation(summary = "Get amenity by ID")
    public ResponseEntity<ApiResponse<AmenityResponse>> getAmenity(@PathVariable UUID id) {
        AmenityResponse response = amenityService.getAmenity(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all amenities with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<AmenityResponse>>> getAllAmenities(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AmenityResponse> response = amenityService.getAllAmenities(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN', 'RESIDENT', 'FACILITY_MANAGER')")
    @Operation(summary = "Get amenities by estate")
    public ResponseEntity<ApiResponse<PagedResponse<AmenityResponse>>> getAmenitiesByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AmenityResponse> response = amenityService.getAmenitiesByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update amenity status")
    public ResponseEntity<ApiResponse<AmenityResponse>> updateAmenityStatus(
            @PathVariable UUID id,
            @RequestParam AmenityStatus status) {
        AmenityResponse response = amenityService.updateAmenityStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Amenity status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Soft-delete an amenity")
    public ResponseEntity<ApiResponse<Void>> deleteAmenity(@PathVariable UUID id) {
        amenityService.deleteAmenity(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Amenity deleted successfully"));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get amenity dashboard")
    public ResponseEntity<ApiResponse<AmenityDashboardResponse>> getDashboard() {
        AmenityDashboardResponse response = amenityService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
