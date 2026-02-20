package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.ArtisanCategory;
import com.strataguard.core.enums.ArtisanStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.governance.ArtisanService;
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
@RequestMapping("/api/v1/artisans")
@RequiredArgsConstructor
@Tag(name = "Artisans", description = "Artisan/vendor registry management")
public class ArtisanController {

    private final ArtisanService artisanService;
    private final ResidentRepository residentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Register a new artisan")
    public ResponseEntity<ApiResponse<ArtisanResponse>> createArtisan(
            @Valid @RequestBody CreateArtisanRequest request) {
        ArtisanResponse response = artisanService.createArtisan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Artisan registered successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Update an artisan")
    public ResponseEntity<ApiResponse<ArtisanResponse>> updateArtisan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateArtisanRequest request) {
        ArtisanResponse response = artisanService.updateArtisan(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Artisan updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get artisan by ID")
    public ResponseEntity<ApiResponse<ArtisanResponse>> getArtisan(@PathVariable UUID id) {
        ArtisanResponse response = artisanService.getArtisan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all artisans")
    public ResponseEntity<ApiResponse<PagedResponse<ArtisanResponse>>> getAllArtisans(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ArtisanResponse> response = artisanService.getAllArtisans(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get artisans by estate")
    public ResponseEntity<ApiResponse<PagedResponse<ArtisanResponse>>> getArtisansByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "averageRating", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ArtisanResponse> response = artisanService.getArtisansByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get artisans by category")
    public ResponseEntity<ApiResponse<PagedResponse<ArtisanResponse>>> getArtisansByCategory(
            @PathVariable ArtisanCategory category,
            @PageableDefault(size = 20, sort = "averageRating", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ArtisanResponse> response = artisanService.getArtisansByCategory(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Search artisans")
    public ResponseEntity<ApiResponse<PagedResponse<ArtisanResponse>>> searchArtisans(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ArtisanResponse> response = artisanService.searchArtisans(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Verify an artisan")
    public ResponseEntity<ApiResponse<ArtisanResponse>> verifyArtisan(@PathVariable UUID id) {
        ArtisanResponse response = artisanService.verifyArtisan(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Artisan verified"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Update artisan status")
    public ResponseEntity<ApiResponse<ArtisanResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam ArtisanStatus status) {
        ArtisanResponse response = artisanService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Status updated"));
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Rate an artisan")
    public ResponseEntity<ApiResponse<ArtisanRatingResponse>> rateArtisan(
            @PathVariable UUID id,
            @Valid @RequestBody RateArtisanRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentId(jwt);
        ArtisanRatingResponse response = artisanService.rateArtisan(id, residentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Rating submitted"));
    }

    @GetMapping("/{id}/ratings")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get artisan ratings")
    public ResponseEntity<ApiResponse<PagedResponse<ArtisanRatingResponse>>> getArtisanRatings(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ArtisanRatingResponse> response = artisanService.getArtisanRatings(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Delete an artisan")
    public ResponseEntity<ApiResponse<Void>> deleteArtisan(@PathVariable UUID id) {
        artisanService.deleteArtisan(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Artisan deleted"));
    }

    private UUID getResidentId(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId));
        return resident.getId();
    }
}
