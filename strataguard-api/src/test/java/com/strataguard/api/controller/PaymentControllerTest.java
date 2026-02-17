package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.payment.InitiatePaymentRequest;
import com.strataguard.core.dto.payment.InitiatePaymentResponse;
import com.strataguard.core.dto.payment.PaymentResponse;
import com.strataguard.core.dto.payment.RecordPaymentRequest;
import com.strataguard.core.dto.payment.WalletResponse;
import com.strataguard.core.dto.payment.WalletTransactionResponse;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.PaymentMethod;
import com.strataguard.core.enums.PaymentProvider;
import com.strataguard.core.enums.PaymentStatus;
import com.strataguard.core.enums.WalletTransactionType;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.billing.PaymentService;
import com.strataguard.service.billing.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    private static final UUID PAYMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INVOICE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESIDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TENANT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID WALLET_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String USER_ID = "user-id-123";
    private static final String USER_EMAIL = "resident@example.com";

    @Mock
    private PaymentService paymentService;

    @Mock
    private WalletService walletService;

    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private PaymentResponse buildPaymentResponse() {
        return PaymentResponse.builder()
                .id(PAYMENT_ID)
                .invoiceId(INVOICE_ID)
                .invoiceNumber("INV-2026-0001")
                .amount(BigDecimal.valueOf(5000))
                .paymentMethod(PaymentMethod.CARD)
                .paymentProvider(PaymentProvider.PAYSTACK)
                .reference("PAY-ref-123")
                .status(PaymentStatus.SUCCESS)
                .paidAt(Instant.now())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("admin")
                .build();
    }

    private PagedResponse<PaymentResponse> buildPagedPaymentResponse() {
        return PagedResponse.<PaymentResponse>builder()
                .content(List.of(buildPaymentResponse()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    private Jwt buildMockJwt() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(USER_ID);
        return jwt;
    }

    private Jwt buildMockJwtWithEmail() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("email")).thenReturn(USER_EMAIL);
        return jwt;
    }

    private void mockResidentLookup() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setUserId(USER_ID);
        when(residentRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.of(resident));
    }

    @Nested
    @DisplayName("POST /initialize - Initialize Payment")
    class InitializePayment {

        @Test
        @DisplayName("should return 201 CREATED with payment initialization data and success message")
        void shouldInitializePaymentSuccessfully() {
            InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .paymentMethod(PaymentMethod.CARD)
                    .callbackUrl("https://example.com/callback")
                    .build();

            InitiatePaymentResponse expectedResponse = InitiatePaymentResponse.builder()
                    .authorizationUrl("https://paystack.com/pay/abc123")
                    .reference("PAY-ref-123")
                    .accessCode("abc123")
                    .build();

            Jwt jwt = buildMockJwtWithEmail();
            when(paymentService.initializePaystackPayment(request, USER_EMAIL)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<InitiatePaymentResponse>> result =
                    paymentController.initializePayment(request, jwt);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Payment initialized successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getAuthorizationUrl()).isEqualTo("https://paystack.com/pay/abc123");

            verify(paymentService).initializePaystackPayment(request, USER_EMAIL);
        }
    }

    @Nested
    @DisplayName("POST /record - Record Manual Payment")
    class RecordManualPayment {

        @Test
        @DisplayName("should return 201 CREATED with payment data and success message")
        void shouldRecordManualPaymentSuccessfully() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .invoiceId(INVOICE_ID)
                    .amount(BigDecimal.valueOf(5000))
                    .paymentMethod(PaymentMethod.CASH)
                    .reference("MANUAL-ref-123")
                    .notes("Cash payment received at office")
                    .build();

            PaymentResponse expectedResponse = buildPaymentResponse();
            expectedResponse.setPaymentMethod(PaymentMethod.CASH);
            expectedResponse.setPaymentProvider(PaymentProvider.MANUAL);

            when(paymentService.recordManualPayment(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PaymentResponse>> result = paymentController.recordPayment(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Payment recorded successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(paymentService).recordManualPayment(request);
        }
    }

    @Nested
    @DisplayName("POST /webhook/paystack - Paystack Webhook")
    class HandlePaystackWebhook {

        @Test
        @DisplayName("should return 200 OK with plain 'OK' string")
        void shouldHandlePaystackWebhookSuccessfully() {
            String payload = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"PAY-ref-123\"}}";
            String signature = "valid-signature-hash";

            ResponseEntity<String> result = paymentController.handlePaystackWebhook(payload, signature);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo("OK");

            verify(paymentService).handlePaystackWebhook(payload, signature);
        }
    }

    @Nested
    @DisplayName("GET / - Get All Payments")
    class GetAllPayments {

        @Test
        @DisplayName("should return 200 OK with paged payment data")
        void shouldGetAllPaymentsSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<PaymentResponse> pagedResponse = buildPagedPaymentResponse();

            when(paymentService.getAllPayments(pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> result =
                    paymentController.getAllPayments(pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(paymentService).getAllPayments(pageable);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Payment by ID")
    class GetPayment {

        @Test
        @DisplayName("should return 200 OK with payment data")
        void shouldGetPaymentSuccessfully() {
            PaymentResponse expectedResponse = buildPaymentResponse();
            when(paymentService.getPayment(PAYMENT_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PaymentResponse>> result = paymentController.getPayment(PAYMENT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(paymentService).getPayment(PAYMENT_ID);
        }
    }

    @Nested
    @DisplayName("GET /invoice/{invoiceId} - Get Payments by Invoice")
    class GetPaymentsByInvoice {

        @Test
        @DisplayName("should return 200 OK with paged payments for invoice")
        void shouldGetPaymentsByInvoiceSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<PaymentResponse> pagedResponse = buildPagedPaymentResponse();

            when(paymentService.getPaymentsByInvoice(INVOICE_ID, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> result =
                    paymentController.getPaymentsByInvoice(INVOICE_ID, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(paymentService).getPaymentsByInvoice(INVOICE_ID, pageable);
        }
    }

    @Nested
    @DisplayName("GET /verify/{reference} - Verify Payment")
    class VerifyPayment {

        @Test
        @DisplayName("should return 200 OK with verified payment data")
        void shouldVerifyPaymentSuccessfully() {
            String reference = "PAY-ref-123";
            PaymentResponse expectedResponse = buildPaymentResponse();

            when(paymentService.verifyPayment(reference)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PaymentResponse>> result = paymentController.verifyPayment(reference);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getReference()).isEqualTo(reference);

            verify(paymentService).verifyPayment(reference);
        }
    }

    @Nested
    @DisplayName("GET /my-payments - Get Current Resident's Payments")
    class GetMyPayments {

        @Test
        @DisplayName("should return 200 OK with paged payments for authenticated resident")
        void shouldGetMyPaymentsSuccessfully() {
            Jwt jwt = buildMockJwt();
            mockResidentLookup();
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<PaymentResponse> pagedResponse = buildPagedPaymentResponse();

            when(paymentService.getAllPayments(pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> result =
                    paymentController.getMyPayments(jwt, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(residentRepository).findByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(paymentService).getAllPayments(pageable);
        }
    }

    @Nested
    @DisplayName("GET /wallet - Get Current Resident's Wallet")
    class GetMyWallet {

        @Test
        @DisplayName("should return 200 OK with wallet data for authenticated resident")
        void shouldGetMyWalletSuccessfully() {
            Jwt jwt = buildMockJwt();
            mockResidentLookup();

            WalletResponse expectedResponse = WalletResponse.builder()
                    .id(WALLET_ID)
                    .residentId(RESIDENT_ID)
                    .residentName("John Doe")
                    .balance(BigDecimal.valueOf(2500))
                    .build();

            when(walletService.getWallet(RESIDENT_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<WalletResponse>> result = paymentController.getMyWallet(jwt);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2500));

            verify(residentRepository).findByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(walletService).getWallet(RESIDENT_ID);
        }
    }

    @Nested
    @DisplayName("GET /wallet/transactions - Get Current Resident's Wallet Transactions")
    class GetMyWalletTransactions {

        @Test
        @DisplayName("should return 200 OK with paged wallet transactions for authenticated resident")
        void shouldGetMyWalletTransactionsSuccessfully() {
            Jwt jwt = buildMockJwt();
            mockResidentLookup();
            Pageable pageable = PageRequest.of(0, 20);

            PagedResponse<WalletTransactionResponse> pagedResponse = PagedResponse.<WalletTransactionResponse>builder()
                    .content(List.of(WalletTransactionResponse.builder()
                            .id(UUID.randomUUID())
                            .walletId(WALLET_ID)
                            .amount(BigDecimal.valueOf(5000))
                            .transactionType(WalletTransactionType.CREDIT_OVERPAYMENT)
                            .referenceId(PAYMENT_ID)
                            .referenceType("PAYMENT")
                            .description("Overpayment from invoice INV-2026-0001")
                            .balanceAfter(BigDecimal.valueOf(5000))
                            .createdAt(Instant.now())
                            .build()))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(walletService.getTransactions(RESIDENT_ID, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<WalletTransactionResponse>>> result =
                    paymentController.getMyWalletTransactions(jwt, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(residentRepository).findByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(walletService).getTransactions(RESIDENT_ID, pageable);
        }
    }

    @Nested
    @DisplayName("POST /wallet/apply - Apply Wallet Balance to Invoice")
    class ApplyWalletToInvoice {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldApplyWalletToInvoiceSuccessfully() {
            Jwt jwt = buildMockJwt();
            mockResidentLookup();

            ResponseEntity<ApiResponse<Void>> result = paymentController.applyWalletToInvoice(INVOICE_ID, jwt);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Wallet balance applied to invoice");
            assertThat(result.getBody().getData()).isNull();

            verify(residentRepository).findByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(paymentService).applyWalletToInvoice(INVOICE_ID, RESIDENT_ID);
        }
    }
}
