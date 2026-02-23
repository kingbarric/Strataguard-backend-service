package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.CreateInvoiceRequest;
import com.strataguard.core.dto.billing.InvoiceResponse;
import com.strataguard.core.dto.billing.InvoiceSummaryResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.*;
import com.strataguard.core.enums.ChargeType;
import com.strataguard.core.enums.InvoiceStatus;
import com.strataguard.core.enums.LevyFrequency;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ChargeInvoiceMapper;
import com.strataguard.infrastructure.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INVOICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CHARGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private ChargeInvoiceRepository invoiceRepository;

    @Mock
    private EstateChargeRepository estateChargeRepository;

    @Mock
    private TenantChargeRepository tenantChargeRepository;

    @Mock
    private EstateChargeExclusionRepository exclusionRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private TenancyRepository tenancyRepository;

    @Mock
    private ChargeInvoiceMapper invoiceMapper;

    @InjectMocks
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(invoiceService, "penaltyRatePerMonth", 0.05);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private EstateCharge buildEstateCharge() {
        EstateCharge charge = new EstateCharge();
        charge.setId(CHARGE_ID);
        charge.setTenantId(TENANT_ID);
        charge.setName("Monthly Maintenance");
        charge.setAmount(new BigDecimal("1000.00"));
        charge.setFrequency(LevyFrequency.MONTHLY);
        charge.setEstateId(ESTATE_ID);
        charge.setActive(true);
        return charge;
    }

    private Unit buildUnit() {
        Unit unit = new Unit();
        unit.setId(UNIT_ID);
        unit.setTenantId(TENANT_ID);
        unit.setUnitNumber("A-101");
        unit.setEstateId(ESTATE_ID);
        return unit;
    }

    private Resident buildResident() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        return resident;
    }

    private Tenancy buildTenancy() {
        Tenancy tenancy = new Tenancy();
        tenancy.setId(UUID.randomUUID());
        tenancy.setTenantId(TENANT_ID);
        tenancy.setResidentId(RESIDENT_ID);
        tenancy.setUnitId(UNIT_ID);
        return tenancy;
    }

    private ChargeInvoice buildInvoice() {
        ChargeInvoice invoice = new ChargeInvoice();
        invoice.setId(INVOICE_ID);
        invoice.setTenantId(TENANT_ID);
        invoice.setInvoiceNumber("INV-202602-000001");
        invoice.setChargeType(ChargeType.ESTATE_CHARGE);
        invoice.setChargeId(CHARGE_ID);
        invoice.setUnitId(UNIT_ID);
        invoice.setResidentId(RESIDENT_ID);
        invoice.setAmount(new BigDecimal("1000.00"));
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setActive(true);
        return invoice;
    }

    private InvoiceResponse buildInvoiceResponse() {
        return InvoiceResponse.builder()
                .id(INVOICE_ID)
                .invoiceNumber("INV-202602-000001")
                .chargeType(ChargeType.ESTATE_CHARGE)
                .chargeId(CHARGE_ID)
                .chargeName("Monthly Maintenance")
                .unitId(UNIT_ID)
                .unitNumber("A-101")
                .residentId(RESIDENT_ID)
                .residentName("John Doe")
                .amount(new BigDecimal("1000.00"))
                .penaltyAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.PENDING)
                .active(true)
                .build();
    }

    private void stubEnrichResponse(ChargeInvoice invoice) {
        InvoiceResponse response = buildInvoiceResponse();
        when(invoiceMapper.toResponse(invoice)).thenReturn(response);
        when(estateChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId()))
                .thenReturn(Optional.of(buildEstateCharge()));
        when(unitRepository.findByIdAndTenantId(invoice.getUnitId(), invoice.getTenantId()))
                .thenReturn(Optional.of(buildUnit()));
        when(residentRepository.findByIdAndTenantId(invoice.getResidentId(), invoice.getTenantId()))
                .thenReturn(Optional.of(buildResident()));
    }

    @Nested
    @DisplayName("createInvoice")
    class CreateInvoice {

        @Test
        @DisplayName("should create invoice successfully")
        void shouldCreateInvoiceSuccessfully() {
            CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                    .chargeId(CHARGE_ID)
                    .chargeType(ChargeType.ESTATE_CHARGE)
                    .unitId(UNIT_ID)
                    .dueDate(LocalDate.now().plusDays(30))
                    .notes("Test invoice")
                    .build();

            EstateCharge charge = buildEstateCharge();
            Unit unit = buildUnit();
            Tenancy tenancy = buildTenancy();
            ChargeInvoice savedInvoice = buildInvoice();

            when(estateChargeRepository.findByIdAndTenantId(CHARGE_ID, TENANT_ID)).thenReturn(Optional.of(charge));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(Optional.of(unit));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(List.of(tenancy));
            when(invoiceRepository.countByInvoiceNumberPrefix(eq(TENANT_ID), any())).thenReturn(0L);
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(savedInvoice);
            stubEnrichResponse(savedInvoice);

            InvoiceResponse result = invoiceService.createInvoice(request);

            assertThat(result).isNotNull();
            assertThat(result.getChargeName()).isEqualTo("Monthly Maintenance");
            assertThat(result.getUnitNumber()).isEqualTo("A-101");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
            verify(invoiceRepository).save(any(ChargeInvoice.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when charge not found")
        void shouldThrowWhenChargeNotFound() {
            CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                    .chargeId(CHARGE_ID)
                    .chargeType(ChargeType.ESTATE_CHARGE)
                    .unitId(UNIT_ID)
                    .dueDate(LocalDate.now().plusDays(30))
                    .build();

            when(estateChargeRepository.findByIdAndTenantId(CHARGE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.createInvoice(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("EstateCharge");

            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when unit not found")
        void shouldThrowWhenUnitNotFound() {
            CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                    .chargeId(CHARGE_ID)
                    .chargeType(ChargeType.ESTATE_CHARGE)
                    .unitId(UNIT_ID)
                    .dueDate(LocalDate.now().plusDays(30))
                    .build();

            EstateCharge charge = buildEstateCharge();
            when(estateChargeRepository.findByIdAndTenantId(CHARGE_ID, TENANT_ID)).thenReturn(Optional.of(charge));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.createInvoice(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Unit");

            verify(invoiceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getInvoice")
    class GetInvoice {

        @Test
        @DisplayName("should return invoice when found")
        void shouldReturnInvoiceWhenFound() {
            ChargeInvoice invoice = buildInvoice();

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));
            stubEnrichResponse(invoice);

            InvoiceResponse result = invoiceService.getInvoice(INVOICE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(INVOICE_ID);
            assertThat(result.getInvoiceNumber()).isEqualTo("INV-202602-000001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.getInvoice(INVOICE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Invoice");
        }
    }

    @Nested
    @DisplayName("getAllInvoices")
    class GetAllInvoices {

        @Test
        @DisplayName("should return paged invoices")
        void shouldReturnPagedInvoices() {
            Pageable pageable = PageRequest.of(0, 10);
            ChargeInvoice invoice = buildInvoice();
            Page<ChargeInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);

            when(invoiceRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            stubEnrichResponse(invoice);

            PagedResponse<InvoiceResponse> result = invoiceService.getAllInvoices(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("cancelInvoice")
    class CancelInvoice {

        @Test
        @DisplayName("should cancel invoice successfully")
        void shouldCancelInvoiceSuccessfully() {
            ChargeInvoice invoice = buildInvoice();
            invoice.setStatus(InvoiceStatus.PENDING);

            ChargeInvoice cancelledInvoice = buildInvoice();
            cancelledInvoice.setStatus(InvoiceStatus.CANCELLED);
            cancelledInvoice.setActive(false);

            InvoiceResponse expectedResponse = buildInvoiceResponse();
            expectedResponse.setStatus(InvoiceStatus.CANCELLED);
            expectedResponse.setActive(false);

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(cancelledInvoice);
            when(invoiceMapper.toResponse(cancelledInvoice)).thenReturn(expectedResponse);
            when(estateChargeRepository.findByIdAndTenantId(cancelledInvoice.getChargeId(), cancelledInvoice.getTenantId()))
                    .thenReturn(Optional.of(buildEstateCharge()));
            when(unitRepository.findByIdAndTenantId(cancelledInvoice.getUnitId(), cancelledInvoice.getTenantId()))
                    .thenReturn(Optional.of(buildUnit()));
            when(residentRepository.findByIdAndTenantId(cancelledInvoice.getResidentId(), cancelledInvoice.getTenantId()))
                    .thenReturn(Optional.of(buildResident()));

            InvoiceResponse result = invoiceService.cancelInvoice(INVOICE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
            assertThat(invoice.isActive()).isFalse();
            verify(invoiceRepository).save(invoice);
        }

        @Test
        @DisplayName("should throw IllegalStateException when invoice is already paid")
        void shouldThrowWhenInvoiceAlreadyPaid() {
            ChargeInvoice invoice = buildInvoice();
            invoice.setStatus(InvoiceStatus.PAID);

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> invoiceService.cancelInvoice(INVOICE_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel a paid invoice");

            verify(invoiceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("applyPenalties")
    class ApplyPenalties {

        @Test
        @DisplayName("should calculate and apply penalties correctly")
        void shouldCalculateAndApplyPenaltiesCorrectly() {
            ChargeInvoice overdueInvoice = buildInvoice();
            overdueInvoice.setDueDate(LocalDate.now().minusMonths(3));
            overdueInvoice.setAmount(new BigDecimal("1000.00"));
            overdueInvoice.setStatus(InvoiceStatus.PENDING);

            when(invoiceRepository.findOverdueByTenantId(eq(TENANT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(overdueInvoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(overdueInvoice);

            int updatedCount = invoiceService.applyPenalties();

            assertThat(updatedCount).isEqualTo(1);
            assertThat(overdueInvoice.getPenaltyAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(overdueInvoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1150.00"));
            assertThat(overdueInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
            verify(invoiceRepository).save(overdueInvoice);
        }

        @Test
        @DisplayName("should apply minimum one month penalty when just past due")
        void shouldApplyMinimumOneMonthPenalty() {
            ChargeInvoice overdueInvoice = buildInvoice();
            overdueInvoice.setDueDate(LocalDate.now().minusDays(5));
            overdueInvoice.setAmount(new BigDecimal("1000.00"));
            overdueInvoice.setStatus(InvoiceStatus.PENDING);

            when(invoiceRepository.findOverdueByTenantId(eq(TENANT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(overdueInvoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(overdueInvoice);

            int updatedCount = invoiceService.applyPenalties();

            assertThat(updatedCount).isEqualTo(1);
            assertThat(overdueInvoice.getPenaltyAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(overdueInvoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1050.00"));
        }

        @Test
        @DisplayName("should return zero when no overdue invoices")
        void shouldReturnZeroWhenNoOverdueInvoices() {
            when(invoiceRepository.findOverdueByTenantId(eq(TENANT_ID), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            int updatedCount = invoiceService.applyPenalties();

            assertThat(updatedCount).isZero();
            verify(invoiceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateInvoicePayment")
    class UpdateInvoicePayment {

        @Test
        @DisplayName("should set status to PARTIAL when partially paid")
        void shouldSetStatusToPartialWhenPartiallyPaid() {
            ChargeInvoice invoice = buildInvoice();
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(new BigDecimal("1000.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(invoice);

            invoiceService.updateInvoicePayment(INVOICE_ID, new BigDecimal("500.00"));

            assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
            verify(invoiceRepository).save(invoice);
        }

        @Test
        @DisplayName("should set status to PAID when fully paid")
        void shouldSetStatusToPaidWhenFullyPaid() {
            ChargeInvoice invoice = buildInvoice();
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(new BigDecimal("1000.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(invoice);

            invoiceService.updateInvoicePayment(INVOICE_ID, new BigDecimal("1000.00"));

            assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
            verify(invoiceRepository).save(invoice);
        }

        @Test
        @DisplayName("should set status to PAID when overpaid")
        void shouldSetStatusToPaidWhenOverpaid() {
            ChargeInvoice invoice = buildInvoice();
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(new BigDecimal("1000.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(invoice);

            invoiceService.updateInvoicePayment(INVOICE_ID, new BigDecimal("1200.00"));

            assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1200.00"));
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        }

        @Test
        @DisplayName("should accumulate payments correctly")
        void shouldAccumulatePaymentsCorrectly() {
            ChargeInvoice invoice = buildInvoice();
            invoice.setPaidAmount(new BigDecimal("300.00"));
            invoice.setTotalAmount(new BigDecimal("1000.00"));

            when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(invoice);

            invoiceService.updateInvoicePayment(INVOICE_ID, new BigDecimal("200.00"));

            assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        }
    }

    @Nested
    @DisplayName("getInvoiceSummary")
    class GetInvoiceSummary {

        @Test
        @DisplayName("should return invoice summary with all aggregated values")
        void shouldReturnInvoiceSummary() {
            when(invoiceRepository.countByTenantId(TENANT_ID)).thenReturn(10L);
            when(invoiceRepository.sumTotalAmountByTenantId(TENANT_ID)).thenReturn(new BigDecimal("50000.00"));
            when(invoiceRepository.sumPaidAmountByTenantId(TENANT_ID)).thenReturn(new BigDecimal("30000.00"));
            when(invoiceRepository.sumPendingAmountByTenantId(TENANT_ID)).thenReturn(new BigDecimal("15000.00"));
            when(invoiceRepository.sumOverdueAmountByTenantId(TENANT_ID)).thenReturn(new BigDecimal("5000.00"));
            when(invoiceRepository.countOverdueByTenantId(TENANT_ID)).thenReturn(3L);

            InvoiceSummaryResponse result = invoiceService.getInvoiceSummary();

            assertThat(result).isNotNull();
            assertThat(result.getTotalInvoices()).isEqualTo(10L);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
            assertThat(result.getPaidAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
            assertThat(result.getPendingAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(result.getOverdueAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.getOverdueCount()).isEqualTo(3L);
        }
    }
}
