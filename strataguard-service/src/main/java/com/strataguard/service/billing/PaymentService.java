package com.strataguard.service.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.payment.*;
import com.strataguard.core.entity.LevyInvoice;
import com.strataguard.core.entity.Payment;
import com.strataguard.core.enums.*;
import com.strataguard.core.exception.PaymentProcessingException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.PaymentMapper;
import com.strataguard.infrastructure.repository.LevyInvoiceRepository;
import com.strataguard.infrastructure.repository.PaymentRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LevyInvoiceRepository invoiceRepository;
    private final ResidentRepository residentRepository;
    private final InvoiceService invoiceService;
    private final WalletService walletService;
    private final PaymentMapper paymentMapper;
    private final WebClient paystackWebClient;
    private final PaystackConfig paystackConfig;
    private final ObjectMapper objectMapper;

    public InitiatePaymentResponse initializePaystackPayment(InitiatePaymentRequest request, String residentEmail) {
        UUID tenantId = TenantContext.requireTenantId();

        LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(request.getInvoiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is already " + invoice.getStatus());
        }

        BigDecimal outstandingAmount = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        String reference = "PAY-" + UUID.randomUUID();

        // Create payment record
        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setInvoiceId(request.getInvoiceId());
        payment.setAmount(outstandingAmount);
        payment.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CARD);
        payment.setPaymentProvider(PaymentProvider.PAYSTACK);
        payment.setReference(reference);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Call Paystack Initialize Transaction
        long amountInKobo = outstandingAmount.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, Object> paystackRequest = new HashMap<>();
        paystackRequest.put("email", residentEmail);
        paystackRequest.put("amount", amountInKobo);
        paystackRequest.put("reference", reference);
        paystackRequest.put("metadata", Map.of(
                "invoice_id", invoice.getId().toString(),
                "invoice_number", invoice.getInvoiceNumber(),
                "tenant_id", tenantId.toString()
        ));
        if (request.getCallbackUrl() != null) {
            paystackRequest.put("callback_url", request.getCallbackUrl());
        }

        try {
            String responseBody = paystackWebClient.post()
                    .uri("/transaction/initialize")
                    .bodyValue(paystackRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode data = responseJson.get("data");

            log.info("Paystack payment initialized: reference={} for invoice={}", reference, invoice.getInvoiceNumber());

            return InitiatePaymentResponse.builder()
                    .authorizationUrl(data.get("authorization_url").asText())
                    .reference(reference)
                    .accessCode(data.get("access_code").asText())
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize Paystack payment: {}", e.getMessage(), e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentProcessingException("Failed to initialize payment with Paystack", e);
        }
    }

    public void handlePaystackWebhook(String payload, String signature) {
        // Verify HMAC-SHA512 signature
        if (!verifyPaystackSignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            throw new PaymentProcessingException("Invalid webhook signature");
        }

        try {
            PaystackWebhookPayload webhookPayload = objectMapper.readValue(payload, PaystackWebhookPayload.class);

            if (!"charge.success".equals(webhookPayload.getEvent())) {
                log.debug("Ignoring Paystack webhook event: {}", webhookPayload.getEvent());
                return;
            }

            PaystackWebhookPayload.PaystackData data = webhookPayload.getData();
            String reference = data.getReference();

            Payment payment = paymentRepository.findByReference(reference)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "reference", reference));

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.debug("Payment already processed: {}", reference);
                return;
            }

            // Set tenant context for downstream operations
            TenantContext.setTenantId(payment.getTenantId());

            try {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderReference(reference);
                payment.setPaidAt(Instant.now());
                payment.setPaymentMethod(mapPaystackChannel(data.getChannel()));
                payment.setMetadata(objectMapper.writeValueAsString(data));
                paymentRepository.save(payment);

                // Update invoice
                invoiceService.updateInvoicePayment(payment.getInvoiceId(), payment.getAmount());

                // Handle overpayment → credit wallet
                LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(payment.getInvoiceId(), payment.getTenantId())
                        .orElse(null);
                if (invoice != null && invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) > 0) {
                    BigDecimal excess = invoice.getPaidAmount().subtract(invoice.getTotalAmount());
                    if (invoice.getResidentId() != null) {
                        walletService.credit(invoice.getResidentId(), excess,
                                WalletTransactionType.CREDIT_OVERPAYMENT,
                                payment.getId(), "PAYMENT",
                                "Overpayment from invoice " + invoice.getInvoiceNumber());
                        // Adjust paidAmount to totalAmount
                        invoice.setPaidAmount(invoice.getTotalAmount());
                        invoiceRepository.save(invoice);
                    }
                }

                log.info("Paystack webhook processed: reference={} status=SUCCESS", reference);
            } finally {
                TenantContext.clear();
            }
        } catch (PaymentProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing Paystack webhook: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process webhook", e);
        }
    }

    public PaymentResponse recordManualPayment(RecordPaymentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(request.getInvoiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is already " + invoice.getStatus());
        }

        String reference = request.getReference() != null ? request.getReference() : "MANUAL-" + UUID.randomUUID();

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setInvoiceId(request.getInvoiceId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentProvider(PaymentProvider.MANUAL);
        payment.setReference(reference);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setNotes(request.getNotes());
        paymentRepository.save(payment);

        // Update invoice payment
        invoiceService.updateInvoicePayment(request.getInvoiceId(), request.getAmount());

        // Handle overpayment → credit wallet
        invoice = invoiceRepository.findByIdAndTenantId(request.getInvoiceId(), tenantId).orElse(null);
        if (invoice != null && invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) > 0) {
            BigDecimal excess = invoice.getPaidAmount().subtract(invoice.getTotalAmount());
            if (invoice.getResidentId() != null) {
                walletService.credit(invoice.getResidentId(), excess,
                        WalletTransactionType.CREDIT_OVERPAYMENT,
                        payment.getId(), "PAYMENT",
                        "Overpayment from invoice " + invoice.getInvoiceNumber());
                invoice.setPaidAmount(invoice.getTotalAmount());
                invoiceRepository.save(invoice);
            }
        }

        log.info("Recorded manual payment: {} for invoice: {} tenant: {}", reference, request.getInvoiceId(), tenantId);
        return enrichResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse verifyPayment(String reference) {
        UUID tenantId = TenantContext.requireTenantId();
        Payment payment = paymentRepository.findByReferenceAndTenantId(reference, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "reference", reference));
        return enrichResponse(payment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getAllPayments(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Payment> page = paymentRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Payment payment = paymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return enrichResponse(payment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsByInvoice(UUID invoiceId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Payment> page = paymentRepository.findByInvoiceIdAndTenantId(invoiceId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public void applyWalletToInvoice(UUID invoiceId, UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();

        LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is already " + invoice.getStatus());
        }

        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        WalletResponse wallet = walletService.getWallet(residentId);

        BigDecimal amountToApply = wallet.getBalance().min(outstanding);
        if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No wallet balance available to apply");
        }

        // Debit wallet
        walletService.debit(residentId, amountToApply,
                WalletTransactionType.DEBIT_INVOICE_PAYMENT,
                invoiceId, "INVOICE",
                "Payment applied to invoice " + invoice.getInvoiceNumber());

        // Record as manual payment
        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amountToApply);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setPaymentProvider(PaymentProvider.MANUAL);
        payment.setReference("WALLET-" + UUID.randomUUID());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setNotes("Payment from wallet balance");
        paymentRepository.save(payment);

        // Update invoice
        invoiceService.updateInvoicePayment(invoiceId, amountToApply);

        log.info("Applied wallet balance {} to invoice {} for resident {}", amountToApply, invoiceId, residentId);
    }

    private boolean verifyPaystackSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    paystackConfig.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Error verifying Paystack signature: {}", e.getMessage());
            return false;
        }
    }

    private PaymentMethod mapPaystackChannel(String channel) {
        if (channel == null) return PaymentMethod.CARD;
        return switch (channel.toLowerCase()) {
            case "card" -> PaymentMethod.CARD;
            case "bank", "bank_transfer" -> PaymentMethod.BANK_TRANSFER;
            case "ussd" -> PaymentMethod.USSD;
            case "mobile_money" -> PaymentMethod.MOBILE_MONEY;
            default -> PaymentMethod.CARD;
        };
    }

    private PaymentResponse enrichResponse(Payment payment) {
        PaymentResponse response = paymentMapper.toResponse(payment);
        invoiceRepository.findByIdAndTenantId(payment.getInvoiceId(), payment.getTenantId())
                .ifPresent(invoice -> response.setInvoiceNumber(invoice.getInvoiceNumber()));
        return response;
    }

    private PagedResponse<PaymentResponse> toPagedResponse(Page<Payment> page) {
        return PagedResponse.<PaymentResponse>builder()
                .content(page.getContent().stream().map(this::enrichResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
