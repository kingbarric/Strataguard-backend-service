package com.strataguard.api.controller;

import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.core.config.TenantContext;
import com.strataguard.service.billing.InvoiceService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management endpoints")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ResidentRepository residentRepository;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'invoice.create')")
    @Operation(summary = "Create a single invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invoice created successfully"));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasPermission(null, 'invoice.create')")
    @Operation(summary = "Bulk generate invoices for all active tenancies")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> bulkGenerateInvoices(
            @Valid @RequestBody BulkInvoiceRequest request) {
        List<InvoiceResponse> responses = invoiceService.bulkGenerateInvoices(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses, "Generated " + responses.size() + " invoices"));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'invoice.read')")
    @Operation(summary = "Get all invoices with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getAllInvoices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<InvoiceResponse> response = invoiceService.getAllInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'invoice.read')")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-invoices")
    @PreAuthorize("hasPermission(null, 'invoice.read')")
    @Operation(summary = "Get current resident's invoices")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getMyInvoices(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentIdFromJwt(jwt);
        PagedResponse<InvoiceResponse> response = invoiceService.getInvoicesByResident(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unit/{unitId}")
    @PreAuthorize("hasPermission(null, 'invoice.read')")
    @Operation(summary = "Get invoices by unit")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getInvoicesByUnit(
            @PathVariable UUID unitId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<InvoiceResponse> response = invoiceService.getInvoicesByUnit(unitId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasPermission(null, 'invoice.summary')")
    @Operation(summary = "Get invoice summary statistics")
    public ResponseEntity<ApiResponse<InvoiceSummaryResponse>> getInvoiceSummary() {
        InvoiceSummaryResponse response = invoiceService.getInvoiceSummary();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasPermission(null, 'invoice.read')")
    @Operation(summary = "Get overdue invoices")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> getOverdueInvoices(
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<InvoiceResponse> response = invoiceService.getOverdueInvoices(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasPermission(null, 'invoice.void')")
    @Operation(summary = "Cancel an invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice cancelled successfully"));
    }

    @PostMapping("/apply-penalties")
    @PreAuthorize("hasPermission(null, 'invoice.update')")
    @Operation(summary = "Apply penalties to overdue invoices")
    public ResponseEntity<ApiResponse<Integer>> applyPenalties() {
        int count = invoiceService.applyPenalties();
        return ResponseEntity.ok(ApiResponse.success(count, "Applied penalties to " + count + " overdue invoices"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasPermission(null, 'invoice.read')")
    @Operation(summary = "Search invoices")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> searchInvoices(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<InvoiceResponse> response = invoiceService.searchInvoices(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getResidentIdFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        return residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId))
                .getId();
    }
}
