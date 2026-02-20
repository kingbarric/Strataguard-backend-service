package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.reporting.*;
import com.strataguard.service.reporting.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Analytics and reporting endpoints")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/occupancy")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Get occupancy report")
    public ResponseEntity<ApiResponse<OccupancyReportResponse>> getOccupancyReport() {
        OccupancyReportResponse response = reportingService.getOccupancyReport();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get revenue report")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport() {
        RevenueReportResponse response = reportingService.getRevenueReport();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/visitor-traffic")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'SECURITY_GUARD', 'SUPER_ADMIN')")
    @Operation(summary = "Get visitor traffic report")
    public ResponseEntity<ApiResponse<VisitorTrafficReportResponse>> getVisitorTrafficReport() {
        VisitorTrafficReportResponse response = reportingService.getVisitorTrafficReport();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/security")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SECURITY_GUARD', 'SUPER_ADMIN')")
    @Operation(summary = "Get security report")
    public ResponseEntity<ApiResponse<SecurityReportResponse>> getSecurityReport() {
        SecurityReportResponse response = reportingService.getSecurityReport();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Get dashboard summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        DashboardSummaryResponse response = reportingService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
