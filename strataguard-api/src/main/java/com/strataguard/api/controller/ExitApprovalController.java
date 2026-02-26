package com.strataguard.api.controller;

import com.strataguard.core.dto.approval.CreateExitApprovalRequest;
import com.strataguard.core.dto.approval.ExitApprovalResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.service.gate.ExitApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exit-approvals")
@RequiredArgsConstructor
@Tag(name = "Exit Approvals", description = "Remote exit approval management")
public class ExitApprovalController {

    private final ExitApprovalService exitApprovalService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'exit_approval.create')")
    @Operation(summary = "Create an exit approval request for remote authorization")
    public ResponseEntity<ApiResponse<ExitApprovalResponse>> createApprovalRequest(
            @Valid @RequestBody CreateExitApprovalRequest request) {
        ExitApprovalResponse response = exitApprovalService.createApprovalRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Exit approval request created"));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasPermission(null, 'exit_approval.manage')")
    @Operation(summary = "Get pending approval requests for the authenticated resident")
    public ResponseEntity<ApiResponse<List<ExitApprovalResponse>>> getPendingApprovals(
            @RequestParam UUID residentId) {
        List<ExitApprovalResponse> response = exitApprovalService.getPendingApprovalsForResident(residentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasPermission(null, 'exit_approval.manage')")
    @Operation(summary = "Approve an exit approval request")
    public ResponseEntity<ApiResponse<ExitApprovalResponse>> approveRequest(@PathVariable UUID id) {
        ExitApprovalResponse response = exitApprovalService.approveRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Exit approval approved"));
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("hasPermission(null, 'exit_approval.manage')")
    @Operation(summary = "Deny an exit approval request")
    public ResponseEntity<ApiResponse<ExitApprovalResponse>> denyRequest(@PathVariable UUID id) {
        ExitApprovalResponse response = exitApprovalService.denyRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Exit approval denied"));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'exit_approval.manage')")
    @Operation(summary = "Check the status of an exit approval request")
    public ResponseEntity<ApiResponse<ExitApprovalResponse>> getApprovalStatus(@PathVariable UUID id) {
        ExitApprovalResponse response = exitApprovalService.getApprovalStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
