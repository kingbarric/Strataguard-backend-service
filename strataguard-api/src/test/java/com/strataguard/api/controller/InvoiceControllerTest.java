package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.BulkInvoiceRequest;
import com.strataguard.core.dto.billing.CreateInvoiceRequest;
import com.strataguard.core.dto.billing.InvoiceResponse;
import com.strataguard.core.dto.billing.InvoiceSummaryResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.InvoiceStatus;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.billing.InvoiceService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceController")
class InvoiceControllerTest {

    private static final UUID INVOICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LEVY_TYPE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNIT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RESIDENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ESTATE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String USER_ID = "user-id-123";

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private InvoiceController invoiceController;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private InvoiceResponse buildInvoiceResponse() {
        return InvoiceResponse.builder()
                .id(INVOICE_ID)
                .invoiceNumber("INV-2026-0001")
                .chargeId(LEVY_TYPE_ID)
                .chargeType(com.strataguard.core.enums.ChargeType.ESTATE_CHARGE)
                .chargeName("Service Charge")
                .unitId(UNIT_ID)
                .unitNumber("A101")
                .residentId(RESIDENT_ID)
                .residentName("John Doe")
                .amount(BigDecimal.valueOf(5000))
                .penaltyAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(5000))
                .paidAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 3, 1))
                .status(InvoiceStatus.PENDING)
                .billingPeriodStart(LocalDate.of(2026, 2, 1))
                .billingPeriodEnd(LocalDate.of(2026, 2, 28))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("admin")
                .build();
    }

    private PagedResponse<InvoiceResponse> buildPagedResponse() {
        return PagedResponse.<InvoiceResponse>builder()
                .content(List.of(buildInvoiceResponse()))
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

    private void mockResidentLookup() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setUserId(USER_ID);
        when(residentRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(Optional.of(resident));
    }

    @Nested
    @DisplayName("POST / - Create Invoice")
    class CreateInvoice {

        @Test
        @DisplayName("should return 201 CREATED with invoice data and success message")
        void shouldCreateInvoiceSuccessfully() {
            CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                    .chargeId(LEVY_TYPE_ID)
                    .chargeType(com.strataguard.core.enums.ChargeType.ESTATE_CHARGE)
                    .unitId(UNIT_ID)
                    .dueDate(LocalDate.of(2026, 3, 1))
                    .notes("Monthly levy")
                    .build();

            InvoiceResponse expectedResponse = buildInvoiceResponse();
            when(invoiceService.createInvoice(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<InvoiceResponse>> result = invoiceController.createInvoice(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Invoice created successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(invoiceService).createInvoice(request);
        }
    }

    @Nested
    @DisplayName("POST /bulk - Bulk Generate Invoices")
    class BulkGenerateInvoices {

        @Test
        @DisplayName("should return 201 CREATED with list of invoices and count message")
        void shouldBulkGenerateInvoicesSuccessfully() {
            BulkInvoiceRequest request = BulkInvoiceRequest.builder()
                    .chargeId(LEVY_TYPE_ID)
                    .chargeType(com.strataguard.core.enums.ChargeType.ESTATE_CHARGE)
                    .estateId(ESTATE_ID)
                    .dueDate(LocalDate.of(2026, 3, 1))
                    .billingPeriodStart(LocalDate.of(2026, 2, 1))
                    .billingPeriodEnd(LocalDate.of(2026, 2, 28))
                    .build();

            List<InvoiceResponse> expectedResponses = List.of(buildInvoiceResponse());
            when(invoiceService.bulkGenerateInvoices(request)).thenReturn(expectedResponses);

            ResponseEntity<ApiResponse<List<InvoiceResponse>>> result =
                    invoiceController.bulkGenerateInvoices(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Generated 1 invoices");
            assertThat(result.getBody().getData()).hasSize(1);
            assertThat(result.getBody().getData()).isEqualTo(expectedResponses);

            verify(invoiceService).bulkGenerateInvoices(request);
        }
    }

    @Nested
    @DisplayName("GET / - Get All Invoices")
    class GetAllInvoices {

        @Test
        @DisplayName("should return 200 OK with paged invoice data")
        void shouldGetAllInvoicesSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<InvoiceResponse> pagedResponse = buildPagedResponse();

            when(invoiceService.getAllInvoices(pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> result =
                    invoiceController.getAllInvoices(pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(invoiceService).getAllInvoices(pageable);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Invoice by ID")
    class GetInvoice {

        @Test
        @DisplayName("should return 200 OK with invoice data")
        void shouldGetInvoiceSuccessfully() {
            InvoiceResponse expectedResponse = buildInvoiceResponse();
            when(invoiceService.getInvoice(INVOICE_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<InvoiceResponse>> result = invoiceController.getInvoice(INVOICE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(invoiceService).getInvoice(INVOICE_ID);
        }
    }

    @Nested
    @DisplayName("GET /my-invoices - Get Current Resident's Invoices")
    class GetMyInvoices {

        @Test
        @DisplayName("should return 200 OK with paged invoices for authenticated resident")
        void shouldGetMyInvoicesSuccessfully() {
            Jwt jwt = buildMockJwt();
            mockResidentLookup();
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<InvoiceResponse> pagedResponse = buildPagedResponse();

            when(invoiceService.getInvoicesByResident(RESIDENT_ID, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> result =
                    invoiceController.getMyInvoices(jwt, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(residentRepository).findByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(invoiceService).getInvoicesByResident(RESIDENT_ID, pageable);
        }
    }

    @Nested
    @DisplayName("GET /unit/{unitId} - Get Invoices by Unit")
    class GetInvoicesByUnit {

        @Test
        @DisplayName("should return 200 OK with paged invoices for unit")
        void shouldGetInvoicesByUnitSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<InvoiceResponse> pagedResponse = buildPagedResponse();

            when(invoiceService.getInvoicesByUnit(UNIT_ID, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> result =
                    invoiceController.getInvoicesByUnit(UNIT_ID, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(invoiceService).getInvoicesByUnit(UNIT_ID, pageable);
        }
    }

    @Nested
    @DisplayName("GET /summary - Get Invoice Summary")
    class GetInvoiceSummary {

        @Test
        @DisplayName("should return 200 OK with invoice summary statistics")
        void shouldGetInvoiceSummarySuccessfully() {
            InvoiceSummaryResponse expectedResponse = InvoiceSummaryResponse.builder()
                    .totalInvoices(10)
                    .totalAmount(BigDecimal.valueOf(50000))
                    .paidAmount(BigDecimal.valueOf(30000))
                    .pendingAmount(BigDecimal.valueOf(15000))
                    .overdueAmount(BigDecimal.valueOf(5000))
                    .overdueCount(2)
                    .build();

            when(invoiceService.getInvoiceSummary()).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<InvoiceSummaryResponse>> result = invoiceController.getInvoiceSummary();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getTotalInvoices()).isEqualTo(10);
            assertThat(result.getBody().getData().getOverdueCount()).isEqualTo(2);

            verify(invoiceService).getInvoiceSummary();
        }
    }

    @Nested
    @DisplayName("GET /overdue - Get Overdue Invoices")
    class GetOverdueInvoices {

        @Test
        @DisplayName("should return 200 OK with paged overdue invoices")
        void shouldGetOverdueInvoicesSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<InvoiceResponse> pagedResponse = buildPagedResponse();

            when(invoiceService.getOverdueInvoices(pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> result =
                    invoiceController.getOverdueInvoices(pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);

            verify(invoiceService).getOverdueInvoices(pageable);
        }
    }

    @Nested
    @DisplayName("POST /{id}/cancel - Cancel Invoice")
    class CancelInvoice {

        @Test
        @DisplayName("should return 200 OK with cancelled invoice data and success message")
        void shouldCancelInvoiceSuccessfully() {
            InvoiceResponse expectedResponse = buildInvoiceResponse();
            expectedResponse.setStatus(InvoiceStatus.CANCELLED);

            when(invoiceService.cancelInvoice(INVOICE_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<InvoiceResponse>> result = invoiceController.cancelInvoice(INVOICE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Invoice cancelled successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(invoiceService).cancelInvoice(INVOICE_ID);
        }
    }

    @Nested
    @DisplayName("POST /apply-penalties - Apply Penalties")
    class ApplyPenalties {

        @Test
        @DisplayName("should return 200 OK with penalty count and success message")
        void shouldApplyPenaltiesSuccessfully() {
            when(invoiceService.applyPenalties()).thenReturn(5);

            ResponseEntity<ApiResponse<Integer>> result = invoiceController.applyPenalties();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Applied penalties to 5 overdue invoices");
            assertThat(result.getBody().getData()).isEqualTo(5);

            verify(invoiceService).applyPenalties();
        }
    }

    @Nested
    @DisplayName("GET /search - Search Invoices")
    class SearchInvoices {

        @Test
        @DisplayName("should return 200 OK with paged search results")
        void shouldSearchInvoicesSuccessfully() {
            String query = "INV-2026";
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<InvoiceResponse> pagedResponse = buildPagedResponse();

            when(invoiceService.searchInvoices(query, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> result =
                    invoiceController.searchInvoices(query, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(invoiceService).searchInvoices(query, pageable);
        }
    }
}
