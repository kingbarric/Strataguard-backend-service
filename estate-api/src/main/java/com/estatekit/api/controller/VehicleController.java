package com.estatekit.api.controller;

import com.estatekit.core.dto.common.ApiResponse;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.vehicle.BulkImportResponse;
import com.estatekit.core.dto.vehicle.CreateVehicleRequest;
import com.estatekit.core.dto.vehicle.UpdateVehicleRequest;
import com.estatekit.core.dto.vehicle.VehicleResponse;
import com.estatekit.service.resident.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Vehicle management endpoints")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Register a new vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vehicle registered successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable UUID id) {
        VehicleResponse response = vehicleService.getVehicle(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all vehicles with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> getAllVehicles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<VehicleResponse> response = vehicleService.getAllVehicles(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/resident/{residentId}")
    @Operation(summary = "Get all vehicles for a resident")
    public ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> getVehiclesByResident(
            @PathVariable UUID residentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<VehicleResponse> response = vehicleService.getVehiclesByResident(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search vehicles by plate number, make, model, or color")
    public ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> searchVehicles(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<VehicleResponse> response = vehicleService.searchVehicles(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Update a vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Soft-delete a vehicle")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vehicle deleted successfully"));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ESTATE_ADMIN')")
    @Operation(summary = "Bulk import vehicles from CSV")
    public ResponseEntity<ApiResponse<BulkImportResponse>> bulkImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        BulkImportResponse response = vehicleService.bulkImport(file.getInputStream());
        return ResponseEntity.ok(ApiResponse.success(response, "Bulk import completed"));
    }
}
