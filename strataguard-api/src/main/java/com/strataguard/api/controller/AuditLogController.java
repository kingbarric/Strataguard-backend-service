package com.strataguard.api.controller;

import com.strataguard.core.dto.audit.AuditLogFilterRequest;
import com.strataguard.core.dto.audit.AuditLogResponse;
import com.strataguard.core.dto.audit.AuditSummaryResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Tamper-evident audit logging and querying")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'audit.read')")
    @Operation(summary = "Get all audit logs (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAllLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AuditLogResponse> response = auditLogService.getAllLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasPermission(null, 'audit.read')")
    @Operation(summary = "Filter audit logs by criteria")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> filterLogs(
            @ModelAttribute AuditLogFilterRequest filter,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AuditLogResponse> response = auditLogService.getLogsByFilter(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasPermission(null, 'audit.read')")
    @Operation(summary = "Get audit history for a specific entity")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AuditLogResponse> response = auditLogService.getEntityHistory(entityType, entityId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasPermission(null, 'audit.read')")
    @Operation(summary = "Get audit log summary statistics")
    public ResponseEntity<ApiResponse<AuditSummaryResponse>> getSummary() {
        AuditSummaryResponse response = auditLogService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/verify")
    @PreAuthorize("hasPermission(null, 'audit.export')")
    @Operation(summary = "Verify audit log hash chain integrity")
    public ResponseEntity<ApiResponse<Boolean>> verifyIntegrity(
            @RequestParam(defaultValue = "100") int limit) {
        boolean valid = auditLogService.verifyIntegrity(limit);
        String message = valid ? "Audit log integrity verified" : "Audit log integrity check FAILED";
        return ResponseEntity.ok(ApiResponse.success(valid, message));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'audit.read')")
    @Operation(summary = "Get audit log entry by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLog(@PathVariable UUID id) {
        AuditLogResponse response = auditLogService.getAuditLog(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
