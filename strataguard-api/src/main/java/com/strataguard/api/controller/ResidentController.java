package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.resident.CreateResidentRequest;
import com.strataguard.core.dto.resident.LinkKeycloakUserRequest;
import com.strataguard.core.dto.resident.ResidentResponse;
import com.strataguard.core.dto.resident.UpdateResidentRequest;
import com.strataguard.service.resident.ResidentService;
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
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
@Tag(name = "Residents", description = "Resident management endpoints")
public class ResidentController {

    private final ResidentService residentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Create a new resident")
    public ResponseEntity<ApiResponse<ResidentResponse>> createResident(
            @Valid @RequestBody CreateResidentRequest request) {
        ResidentResponse response = residentService.createResident(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Resident created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resident by ID")
    public ResponseEntity<ApiResponse<ResidentResponse>> getResident(@PathVariable UUID id) {
        ResidentResponse response = residentService.getResident(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all residents with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ResidentResponse>>> getAllResidents(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ResidentResponse> response = residentService.getAllResidents(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search residents by name, email, or phone")
    public ResponseEntity<ApiResponse<PagedResponse<ResidentResponse>>> searchResidents(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ResidentResponse> response = residentService.searchResidents(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's resident profile")
    public ResponseEntity<ApiResponse<ResidentResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        ResidentResponse response = residentService.getByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Update a resident")
    public ResponseEntity<ApiResponse<ResidentResponse>> updateResident(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResidentRequest request) {
        ResidentResponse response = residentService.updateResident(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Resident updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Soft-delete a resident")
    public ResponseEntity<ApiResponse<Void>> deleteResident(@PathVariable UUID id) {
        residentService.deleteResident(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Resident deleted successfully"));
    }

    @PostMapping("/{id}/link-user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Link a Keycloak user to a resident profile")
    public ResponseEntity<ApiResponse<ResidentResponse>> linkKeycloakUser(
            @PathVariable UUID id,
            @Valid @RequestBody LinkKeycloakUserRequest request) {
        ResidentResponse response = residentService.linkKeycloakUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Keycloak user linked successfully"));
    }
}
