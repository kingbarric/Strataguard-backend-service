package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.utility.*;
import com.strataguard.service.utility.SharedUtilityCostService;
import com.strataguard.service.utility.UtilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utilities")
@RequiredArgsConstructor
@Tag(name = "Utilities", description = "Utility meter and reading management endpoints")
public class UtilityController {

    private final UtilityService utilityService;
    private final SharedUtilityCostService sharedUtilityCostService;

    // --- Meters ---

    @PostMapping("/meters")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Create a utility meter")
    public ResponseEntity<ApiResponse<UtilityMeterResponse>> createMeter(
            @Valid @RequestBody CreateUtilityMeterRequest request) {
        UtilityMeterResponse response = utilityService.createMeter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Utility meter created successfully"));
    }

    @PutMapping("/meters/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Update a utility meter")
    public ResponseEntity<ApiResponse<UtilityMeterResponse>> updateMeter(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUtilityMeterRequest request) {
        UtilityMeterResponse response = utilityService.updateMeter(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Utility meter updated successfully"));
    }

    @GetMapping("/meters/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get utility meter by ID")
    public ResponseEntity<ApiResponse<UtilityMeterResponse>> getMeter(@PathVariable UUID id) {
        UtilityMeterResponse response = utilityService.getMeter(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/meters/unit/{unitId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get meters by unit")
    public ResponseEntity<ApiResponse<PagedResponse<UtilityMeterResponse>>> getMetersByUnit(
            @PathVariable UUID unitId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<UtilityMeterResponse> response = utilityService.getMetersByUnit(unitId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/meters/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get meters by estate")
    public ResponseEntity<ApiResponse<PagedResponse<UtilityMeterResponse>>> getMetersByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<UtilityMeterResponse> response = utilityService.getMetersByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- Readings ---

    @PostMapping("/readings")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Record a utility reading")
    public ResponseEntity<ApiResponse<UtilityReadingResponse>> recordReading(
            @Valid @RequestBody RecordReadingRequest request) {
        UtilityReadingResponse response = utilityService.recordReading(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reading recorded successfully"));
    }

    @PostMapping("/readings/{id}/validate")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Validate a utility reading")
    public ResponseEntity<ApiResponse<UtilityReadingResponse>> validateReading(@PathVariable UUID id) {
        UtilityReadingResponse response = utilityService.validateReading(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Reading validated"));
    }

    @GetMapping("/readings/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get readings by meter")
    public ResponseEntity<ApiResponse<PagedResponse<UtilityReadingResponse>>> getReadingsByMeter(
            @PathVariable UUID meterId,
            @PageableDefault(size = 20, sort = "readingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<UtilityReadingResponse> response = utilityService.getReadingsByMeter(meterId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/readings/unit/{unitId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get readings by unit and period")
    public ResponseEntity<ApiResponse<List<UtilityReadingResponse>>> getReadingsByUnit(
            @PathVariable UUID unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        List<UtilityReadingResponse> response = utilityService.getReadingsByUnitAndPeriod(unitId, periodStart, periodEnd);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/meters/{meterId}/trend")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get consumption trend for a meter")
    public ResponseEntity<ApiResponse<ConsumptionTrendResponse>> getConsumptionTrend(
            @PathVariable UUID meterId,
            @RequestParam(defaultValue = "12") int months) {
        ConsumptionTrendResponse response = utilityService.getConsumptionTrend(meterId, months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/statements/unit/{unitId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'RESIDENT')")
    @Operation(summary = "Get utility statement for a unit")
    public ResponseEntity<ApiResponse<UtilityStatementResponse>> getUtilityStatement(
            @PathVariable UUID unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        UtilityStatementResponse response = utilityService.getUtilityStatement(unitId, periodStart, periodEnd);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/readings/generate-invoices")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Generate invoices from validated readings")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generateInvoices() {
        int count = utilityService.generateInvoicesFromReadings();
        return ResponseEntity.ok(ApiResponse.success(Map.of("invoicesGenerated", count), "Invoices generated successfully"));
    }

    // --- Shared Costs ---

    @PostMapping("/shared-costs")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Create a shared utility cost")
    public ResponseEntity<ApiResponse<SharedUtilityCostResponse>> createSharedCost(
            @Valid @RequestBody CreateSharedUtilityCostRequest request) {
        SharedUtilityCostResponse response = sharedUtilityCostService.createSharedCost(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Shared cost created successfully"));
    }

    @GetMapping("/shared-costs/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get shared utility cost by ID")
    public ResponseEntity<ApiResponse<SharedUtilityCostResponse>> getSharedCost(@PathVariable UUID id) {
        SharedUtilityCostResponse response = sharedUtilityCostService.getSharedCost(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/shared-costs/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get shared costs by estate")
    public ResponseEntity<ApiResponse<PagedResponse<SharedUtilityCostResponse>>> getSharedCostsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<SharedUtilityCostResponse> response = sharedUtilityCostService.getSharedCostsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/shared-costs/{id}/generate-invoices")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Generate invoices for a shared cost")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generateSharedCostInvoices(@PathVariable UUID id) {
        int count = sharedUtilityCostService.generateInvoicesForSharedCost(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("invoicesGenerated", count), "Invoices generated successfully"));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get utility dashboard")
    public ResponseEntity<ApiResponse<UtilityDashboardResponse>> getDashboard() {
        UtilityDashboardResponse response = utilityService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
