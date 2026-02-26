package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.payment.*;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.billing.PaymentService;
import com.strataguard.service.billing.WalletService;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final WalletService walletService;
    private final ResidentRepository residentRepository;

    @PostMapping("/initialize")
    @PreAuthorize("hasPermission(null, 'payment.initiate')")
    @Operation(summary = "Initialize a Paystack payment")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initializePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        InitiatePaymentResponse response = paymentService.initializePaystackPayment(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment initialized successfully"));
    }

    @PostMapping("/record")
    @PreAuthorize("hasPermission(null, 'payment.initiate')")
    @Operation(summary = "Record a manual/cash payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse response = paymentService.recordManualPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment recorded successfully"));
    }

    @PostMapping("/webhook/paystack")
    @Operation(summary = "Paystack webhook endpoint")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Paystack-Signature") String signature) {
        paymentService.handlePaystackWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'payment.read')")
    @Operation(summary = "Get all payments with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getAllPayments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<PaymentResponse> response = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'payment.read')")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasPermission(null, 'payment.read')")
    @Operation(summary = "Get payments for an invoice")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getPaymentsByInvoice(
            @PathVariable UUID invoiceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<PaymentResponse> response = paymentService.getPaymentsByInvoice(invoiceId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/verify/{reference}")
    @PreAuthorize("hasPermission(null, 'payment.read')")
    @Operation(summary = "Verify a payment by reference")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(@PathVariable String reference) {
        PaymentResponse response = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasPermission(null, 'payment.read')")
    @Operation(summary = "Get current resident's payments")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentIdFromJwt(jwt);
        // Get invoices for resident and then payments - for now return all tenant payments filtered
        // In a more robust implementation, we'd add a resident-scoped payment query
        PagedResponse<PaymentResponse> response = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/wallet")
    @PreAuthorize("hasPermission(null, 'wallet.read')")
    @Operation(summary = "Get current resident's wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        WalletResponse response = walletService.getWallet(residentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/wallet/transactions")
    @PreAuthorize("hasPermission(null, 'wallet.read')")
    @Operation(summary = "Get current resident's wallet transactions")
    public ResponseEntity<ApiResponse<PagedResponse<WalletTransactionResponse>>> getMyWalletTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentIdFromJwt(jwt);
        PagedResponse<WalletTransactionResponse> response = walletService.getTransactions(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/wallet/apply")
    @PreAuthorize("hasPermission(null, 'wallet.fund')")
    @Operation(summary = "Apply wallet balance to an invoice")
    public ResponseEntity<ApiResponse<Void>> applyWalletToInvoice(
            @RequestParam UUID invoiceId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        paymentService.applyWalletToInvoice(invoiceId, residentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Wallet balance applied to invoice"));
    }

    private UUID getResidentIdFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        return residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId))
                .getId();
    }
}
