package com.strataguard.service.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.payment.PaymentResponse;
import com.strataguard.core.dto.payment.RecordPaymentRequest;
import com.strataguard.core.dto.payment.WalletResponse;
import com.strataguard.core.entity.LevyInvoice;
import com.strataguard.core.entity.Payment;
import com.strataguard.core.enums.*;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.PaymentMapper;
import com.strataguard.infrastructure.repository.LevyInvoiceRepository;
import com.strataguard.infrastructure.repository.PaymentRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID INVOICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID LEVY_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LevyInvoiceRepository invoiceRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private WebClient paystackWebClient;

    @Mock
    private PaystackConfig paystackConfig;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private LevyInvoice buildInvoice(InvoiceStatus status, BigDecimal paidAmount) {
        LevyInvoice invoice = new LevyInvoice();
        invoice.setId(INVOICE_ID);
        invoice.setTenantId(TENANT_ID);
        invoice.setInvoiceNumber("INV-202602-000001");
        invoice.setLevyTypeId(LEVY_TYPE_ID);
        invoice.setUnitId(UNIT_ID);
        invoice.setResidentId(RESIDENT_ID);
        invoice.setAmount(new BigDecimal("1000.00"));
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setPaidAmount(paidAmount);
        invoice.setStatus(status);
        invoice.setActive(true);
        return invoice;
    }

    private Payment buildPayment() {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setTenantId(TENANT_ID);
        payment.setInvoiceId(INVOICE_ID);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        payment.setPaymentProvider(PaymentProvider.MANUAL);
        payment.setReference("MANUAL-test-ref");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setActive(true);
        return payment;
    }

    private PaymentResponse buildPaymentResponse() {
        return PaymentResponse.builder()
                .id(PAYMENT_ID)
                .invoiceId(INVOICE_ID)
                .invoiceNumber("INV-202602-000001")
                .amount(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentProvider(PaymentProvider.MANUAL)
                .reference("MANUAL-test-ref")
                .status(PaymentStatus.SUCCESS)
                .paidAt(Instant.now())
                .active(true)
                .build();
    }

    private void stubEnrichResponse(Payment payment) {
        PaymentResponse response = buildPaymentResponse();
        when(paymentMapper.toResponse(payment)).thenReturn(response);
        LevyInvoice invoice = buildInvoice(InvoiceStatus.PENDING, BigDecimal.ZERO);
        when(invoiceRepository.findByIdAndTenantId(payment.getInvoiceId(), payment.getTenantId()))
                .thenReturn(Optional.of(invoice));
    }

    @Nested
    @DisplayName("recordManualPayment")
    class RecordManualPayment {

        @Test
        @DisplayName("should record manual payment successfully")
        void shouldRecordManualPaymentSuccessfully() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .reference("MANUAL-test-ref")
                    .notes("Bank deposit")
                    .build();

            LevyInvoice invoice = buildInvoice(InvoiceStatus.PENDING, BigDecimal.ZERO);
            // After payment update, invoice shows paid = totalAmount (no overpayment)
            LevyInvoice updatedInvoice = buildInvoice(InvoiceStatus.PAID, new BigDecimal("1000.00"));

            Payment savedPayment = buildPayment();

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(invoice))        // first call: initial check
                    .thenReturn(Optional.of(updatedInvoice)); // second call: after updateInvoicePayment
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(buildPaymentResponse());

            PaymentResponse result = paymentService.recordManualPayment(request);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(result.getPaymentProvider()).isEqualTo(PaymentProvider.MANUAL);

            verify(paymentRepository).save(any(Payment.class));
            verify(invoiceService).updateInvoicePayment(INVOICE_ID, new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("should throw IllegalStateException when invoice is already paid")
        void shouldThrowWhenInvoiceAlreadyPaid() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .amount(new BigDecimal("500.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .build();

            LevyInvoice paidInvoice = buildInvoice(InvoiceStatus.PAID, new BigDecimal("1000.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(paidInvoice));

            assertThatThrownBy(() -> paymentService.recordManualPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already")
                    .hasMessageContaining("PAID");

            verify(paymentRepository, never()).save(any());
            verify(invoiceService, never()).updateInvoicePayment(any(), any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when invoice is cancelled")
        void shouldThrowWhenInvoiceCancelled() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .amount(new BigDecimal("500.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .build();

            LevyInvoice cancelledInvoice = buildInvoice(InvoiceStatus.CANCELLED, BigDecimal.ZERO);

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(cancelledInvoice));

            assertThatThrownBy(() -> paymentService.recordManualPayment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already")
                    .hasMessageContaining("CANCELLED");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should credit wallet when overpayment occurs")
        void shouldCreditWalletOnOverpayment() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .amount(new BigDecimal("1200.00"))
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .build();

            LevyInvoice invoice = buildInvoice(InvoiceStatus.PENDING, BigDecimal.ZERO);
            // After updateInvoicePayment, paidAmount exceeds totalAmount
            LevyInvoice overpaidInvoice = buildInvoice(InvoiceStatus.PAID, new BigDecimal("1200.00"));

            Payment savedPayment = buildPayment();
            savedPayment.setAmount(new BigDecimal("1200.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(invoice))           // first call: initial check
                    .thenReturn(Optional.of(overpaidInvoice));   // second call: after updateInvoicePayment (also reused by enrichResponse)
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(buildPaymentResponse());
            when(invoiceRepository.save(any(LevyInvoice.class))).thenReturn(overpaidInvoice);

            paymentService.recordManualPayment(request);

            // Excess = 1200 - 1000 = 200
            verify(walletService).credit(
                    eq(RESIDENT_ID),
                    eq(new BigDecimal("200.00")),
                    eq(WalletTransactionType.CREDIT_OVERPAYMENT),
                    any(), // payment.getId() is null since save() return value not captured
                    eq("PAYMENT"),
                    any(String.class)
            );
            // Invoice paidAmount adjusted to totalAmount
            verify(invoiceRepository).save(overpaidInvoice);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when invoice not found")
        void shouldThrowWhenInvoiceNotFound() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .amount(new BigDecimal("500.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .build();

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.recordManualPayment(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Invoice");
        }
    }

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPayment {

        @Test
        @DisplayName("should return payment when reference found")
        void shouldReturnPaymentWhenFound() {
            String reference = "MANUAL-test-ref";
            Payment payment = buildPayment();

            when(paymentRepository.findByReferenceAndTenantId(reference, TENANT_ID))
                    .thenReturn(Optional.of(payment));
            stubEnrichResponse(payment);

            PaymentResponse result = paymentService.verifyPayment(reference);

            assertThat(result).isNotNull();
            assertThat(result.getReference()).isEqualTo(reference);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when reference not found")
        void shouldThrowWhenReferenceNotFound() {
            String reference = "NONEXISTENT-REF";

            when(paymentRepository.findByReferenceAndTenantId(reference, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.verifyPayment(reference))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment");
        }
    }

    @Nested
    @DisplayName("getAllPayments")
    class GetAllPayments {

        @Test
        @DisplayName("should return paged payments")
        void shouldReturnPagedPayments() {
            Pageable pageable = PageRequest.of(0, 10);
            Payment payment = buildPayment();
            Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);

            when(paymentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            stubEnrichResponse(payment);

            PagedResponse<PaymentResponse> result = paymentService.getAllPayments(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPaymentWhenFound() {
            Payment payment = buildPayment();

            when(paymentRepository.findByIdAndTenantId(PAYMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(payment));
            stubEnrichResponse(payment);

            PaymentResponse result = paymentService.getPayment(PAYMENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PAYMENT_ID);
            assertThat(result.getInvoiceNumber()).isEqualTo("INV-202602-000001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findByIdAndTenantId(PAYMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(PAYMENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Payment");
        }
    }

    @Nested
    @DisplayName("getPaymentsByInvoice")
    class GetPaymentsByInvoice {

        @Test
        @DisplayName("should return payments for a specific invoice")
        void shouldReturnPaymentsByInvoice() {
            Pageable pageable = PageRequest.of(0, 10);
            Payment payment = buildPayment();
            Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);

            when(paymentRepository.findByInvoiceIdAndTenantId(INVOICE_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            stubEnrichResponse(payment);

            PagedResponse<PaymentResponse> result = paymentService.getPaymentsByInvoice(INVOICE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getInvoiceId()).isEqualTo(INVOICE_ID);
        }
    }

    @Nested
    @DisplayName("applyWalletToInvoice")
    class ApplyWalletToInvoice {

        @Test
        @DisplayName("should apply wallet balance to invoice successfully")
        void shouldApplyWalletToInvoiceSuccessfully() {
            LevyInvoice invoice = buildInvoice(InvoiceStatus.PENDING, BigDecimal.ZERO);
            WalletResponse walletResponse = WalletResponse.builder()
                    .id(UUID.randomUUID())
                    .residentId(RESIDENT_ID)
                    .balance(new BigDecimal("500.00"))
                    .build();

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(invoice));
            when(walletService.getWallet(RESIDENT_ID)).thenReturn(walletResponse);
            when(paymentRepository.save(any(Payment.class))).thenReturn(buildPayment());

            paymentService.applyWalletToInvoice(INVOICE_ID, RESIDENT_ID);

            // wallet balance (500) < outstanding (1000), so applies 500
            verify(walletService).debit(
                    eq(RESIDENT_ID),
                    eq(new BigDecimal("500.00")),
                    eq(WalletTransactionType.DEBIT_INVOICE_PAYMENT),
                    eq(INVOICE_ID),
                    eq("INVOICE"),
                    any(String.class)
            );
            verify(paymentRepository).save(any(Payment.class));
            verify(invoiceService).updateInvoicePayment(INVOICE_ID, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("should apply only outstanding amount when wallet has more than enough")
        void shouldApplyOnlyOutstandingWhenWalletHasExcess() {
            LevyInvoice invoice = buildInvoice(InvoiceStatus.PARTIAL, new BigDecimal("700.00"));
            // Outstanding = 1000 - 700 = 300
            WalletResponse walletResponse = WalletResponse.builder()
                    .id(UUID.randomUUID())
                    .residentId(RESIDENT_ID)
                    .balance(new BigDecimal("500.00"))
                    .build();

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(invoice));
            when(walletService.getWallet(RESIDENT_ID)).thenReturn(walletResponse);
            when(paymentRepository.save(any(Payment.class))).thenReturn(buildPayment());

            paymentService.applyWalletToInvoice(INVOICE_ID, RESIDENT_ID);

            // min(500, 300) = 300
            verify(walletService).debit(
                    eq(RESIDENT_ID),
                    eq(new BigDecimal("300.00")),
                    eq(WalletTransactionType.DEBIT_INVOICE_PAYMENT),
                    eq(INVOICE_ID),
                    eq("INVOICE"),
                    any(String.class)
            );
            verify(invoiceService).updateInvoicePayment(INVOICE_ID, new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("should throw IllegalStateException when invoice is already paid")
        void shouldThrowWhenInvoiceAlreadyPaid() {
            LevyInvoice paidInvoice = buildInvoice(InvoiceStatus.PAID, new BigDecimal("1000.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(paidInvoice));

            assertThatThrownBy(() -> paymentService.applyWalletToInvoice(INVOICE_ID, RESIDENT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already")
                    .hasMessageContaining("PAID");

            verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when no wallet balance available")
        void shouldThrowWhenNoWalletBalance() {
            LevyInvoice invoice = buildInvoice(InvoiceStatus.PENDING, BigDecimal.ZERO);
            WalletResponse walletResponse = WalletResponse.builder()
                    .id(UUID.randomUUID())
                    .residentId(RESIDENT_ID)
                    .balance(BigDecimal.ZERO)
                    .build();

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.of(invoice));
            when(walletService.getWallet(RESIDENT_ID)).thenReturn(walletResponse);

            assertThatThrownBy(() -> paymentService.applyWalletToInvoice(INVOICE_ID, RESIDENT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No wallet balance available");

            verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when invoice not found")
        void shouldThrowWhenInvoiceNotFound() {
            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.applyWalletToInvoice(INVOICE_ID, RESIDENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Invoice");
        }
    }
}
