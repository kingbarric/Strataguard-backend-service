package com.estatekit.api.controller;

import com.estatekit.core.dto.common.ApiResponse;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.estate.CreateEstateRequest;
import com.estatekit.core.dto.estate.EstateResponse;
import com.estatekit.core.dto.estate.UpdateEstateRequest;
import com.estatekit.service.estate.EstateService;
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
@RequestMapping("/api/v1/estates")
@RequiredArgsConstructor
@Tag(name = "Estates", description = "Estate management endpoints")
public class EstateController {

    private final EstateService estateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Create a new estate")
    public ResponseEntity<ApiResponse<EstateResponse>> createEstate(
            @Valid @RequestBody CreateEstateRequest request) {
        EstateResponse response = estateService.createEstate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Estate created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get estate by ID")
    public ResponseEntity<ApiResponse<EstateResponse>> getEstate(@PathVariable UUID id) {
        EstateResponse response = estateService.getEstate(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all estates with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<EstateResponse>>> getAllEstates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<EstateResponse> response = estateService.getAllEstates(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search estates by name, address, or city")
    public ResponseEntity<ApiResponse<PagedResponse<EstateResponse>>> searchEstates(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<EstateResponse> response = estateService.searchEstates(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Update an estate")
    public ResponseEntity<ApiResponse<EstateResponse>> updateEstate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEstateRequest request) {
        EstateResponse response = estateService.updateEstate(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Estate updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Soft-delete an estate")
    public ResponseEntity<ApiResponse<Void>> deleteEstate(@PathVariable UUID id) {
        estateService.deleteEstate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Estate deleted successfully"));
    }
}
