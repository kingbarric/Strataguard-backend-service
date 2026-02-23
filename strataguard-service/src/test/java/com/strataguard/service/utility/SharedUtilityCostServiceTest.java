package com.strataguard.service.utility;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.utility.CreateSharedUtilityCostRequest;
import com.strataguard.core.dto.utility.SharedUtilityCostResponse;
import com.strataguard.core.entity.*;
import com.strataguard.core.enums.CostSplitMethod;
import com.strataguard.core.enums.UtilityType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.SharedUtilityCostMapper;
import com.strataguard.infrastructure.repository.*;
import com.strataguard.service.notification.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class SharedUtilityCostServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SHARED_COST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock
    private SharedUtilityCostRepository sharedCostRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private TenancyRepository tenancyRepository;

    @Mock
    private ChargeInvoiceRepository invoiceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SharedUtilityCostMapper sharedCostMapper;

    @InjectMocks
    private SharedUtilityCostService sharedUtilityCostService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Unit buildUnit(UUID unitId, String unitNumber, Double squareMeters) {
        Unit unit = new Unit();
        unit.setId(unitId);
        unit.setTenantId(TENANT_ID);
        unit.setUnitNumber(unitNumber);
        unit.setEstateId(ESTATE_ID);
        unit.setSquareMeters(squareMeters);
        return unit;
    }

    private Unit buildUnit() {
        return buildUnit(UNIT_ID, "A-101", 100.0);
    }

    private Tenancy buildTenancy(UUID unitId) {
        Tenancy tenancy = new Tenancy();
        tenancy.setId(UUID.randomUUID());
        tenancy.setTenantId(TENANT_ID);
        tenancy.setResidentId(RESIDENT_ID);
        tenancy.setUnitId(unitId);
        return tenancy;
    }

    private SharedUtilityCost buildSharedCost() {
        SharedUtilityCost cost = new SharedUtilityCost();
        cost.setId(SHARED_COST_ID);
        cost.setTenantId(TENANT_ID);
        cost.setEstateId(ESTATE_ID);
        cost.setUtilityType(UtilityType.ELECTRICITY);
        cost.setTotalCost(new BigDecimal("3000.00"));
        cost.setSplitMethod(CostSplitMethod.EQUAL);
        cost.setTotalUnitsParticipating(3);
        cost.setCostPerUnit(new BigDecimal("1000.00"));
        cost.setBillingPeriodStart(LocalDate.of(2026, 1, 1));
        cost.setBillingPeriodEnd(LocalDate.of(2026, 1, 31));
        cost.setDescription("January electricity");
        cost.setInvoicesGenerated(false);
        return cost;
    }

    private SharedUtilityCostResponse buildSharedCostResponse() {
        return SharedUtilityCostResponse.builder()
                .id(SHARED_COST_ID)
                .estateId(ESTATE_ID)
                .utilityType(UtilityType.ELECTRICITY)
                .totalCost(new BigDecimal("3000.00"))
                .splitMethod(CostSplitMethod.EQUAL)
                .totalUnitsParticipating(3)
                .costPerUnit(new BigDecimal("1000.00"))
                .billingPeriodStart(LocalDate.of(2026, 1, 1))
                .billingPeriodEnd(LocalDate.of(2026, 1, 31))
                .description("January electricity")
                .invoicesGenerated(false)
                .build();
    }

    private CreateSharedUtilityCostRequest buildCreateRequest(CostSplitMethod splitMethod) {
        return CreateSharedUtilityCostRequest.builder()
                .estateId(ESTATE_ID)
                .utilityType(UtilityType.ELECTRICITY)
                .totalCost(new BigDecimal("3000.00"))
                .splitMethod(splitMethod)
                .billingPeriodStart(LocalDate.of(2026, 1, 1))
                .billingPeriodEnd(LocalDate.of(2026, 1, 31))
                .description("January electricity")
                .build();
    }

    @Nested
    @DisplayName("createSharedCost")
    class CreateSharedCost {

        @Test
        @DisplayName("should create shared cost with EQUAL split successfully")
        void shouldCreateSharedCostWithEqualSplit() {
            CreateSharedUtilityCostRequest request = buildCreateRequest(CostSplitMethod.EQUAL);
            Unit unit1 = buildUnit(UNIT_ID, "A-101", 100.0);
            Unit unit2 = buildUnit(UUID.randomUUID(), "A-102", 200.0);
            Unit unit3 = buildUnit(UUID.randomUUID(), "A-103", 150.0);
            List<Unit> allUnits = List.of(unit1, unit2, unit3);

            SharedUtilityCost saved = buildSharedCost();
            SharedUtilityCostResponse expectedResponse = buildSharedCostResponse();

            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(allUnits);
            when(tenancyRepository.findActiveByUnitIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(List.of(buildTenancy(UNIT_ID)));
            when(sharedCostRepository.save(any(SharedUtilityCost.class))).thenReturn(saved);
            when(sharedCostMapper.toResponse(saved)).thenReturn(expectedResponse);

            SharedUtilityCostResponse result = sharedUtilityCostService.createSharedCost(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SHARED_COST_ID);
            assertThat(result.getEstateId()).isEqualTo(ESTATE_ID);
            assertThat(result.getUtilityType()).isEqualTo(UtilityType.ELECTRICITY);
            assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(result.getSplitMethod()).isEqualTo(CostSplitMethod.EQUAL);
            verify(sharedCostRepository).save(any(SharedUtilityCost.class));
        }

        @Test
        @DisplayName("should create shared cost with BY_SQUARE_METERS split successfully")
        void shouldCreateSharedCostWithSquareMetersSplit() {
            CreateSharedUtilityCostRequest request = buildCreateRequest(CostSplitMethod.BY_SQUARE_METERS);
            Unit unit1 = buildUnit(UNIT_ID, "A-101", 100.0);
            Unit unit2 = buildUnit(UUID.randomUUID(), "A-102", 200.0);
            List<Unit> allUnits = List.of(unit1, unit2);

            SharedUtilityCost saved = buildSharedCost();
            saved.setSplitMethod(CostSplitMethod.BY_SQUARE_METERS);
            SharedUtilityCostResponse expectedResponse = buildSharedCostResponse();
            expectedResponse.setSplitMethod(CostSplitMethod.BY_SQUARE_METERS);

            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(allUnits);
            when(tenancyRepository.findActiveByUnitIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(List.of(buildTenancy(UNIT_ID)));
            when(sharedCostRepository.save(any(SharedUtilityCost.class))).thenReturn(saved);
            when(sharedCostMapper.toResponse(saved)).thenReturn(expectedResponse);

            SharedUtilityCostResponse result = sharedUtilityCostService.createSharedCost(request);

            assertThat(result).isNotNull();
            assertThat(result.getSplitMethod()).isEqualTo(CostSplitMethod.BY_SQUARE_METERS);
            verify(sharedCostRepository).save(any(SharedUtilityCost.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when no occupied units")
        void shouldThrowWhenNoOccupiedUnits() {
            CreateSharedUtilityCostRequest request = buildCreateRequest(CostSplitMethod.EQUAL);
            Unit unit = buildUnit();

            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(List.of(unit));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> sharedUtilityCostService.createSharedCost(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No occupied units found");

            verify(sharedCostRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getSharedCost")
    class GetSharedCost {

        @Test
        @DisplayName("should return shared cost when found")
        void shouldReturnSharedCostWhenFound() {
            SharedUtilityCost cost = buildSharedCost();
            SharedUtilityCostResponse expectedResponse = buildSharedCostResponse();

            when(sharedCostRepository.findByIdAndTenantId(SHARED_COST_ID, TENANT_ID))
                    .thenReturn(Optional.of(cost));
            when(sharedCostMapper.toResponse(cost)).thenReturn(expectedResponse);

            SharedUtilityCostResponse result = sharedUtilityCostService.getSharedCost(SHARED_COST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SHARED_COST_ID);
            assertThat(result.getUtilityType()).isEqualTo(UtilityType.ELECTRICITY);
            assertThat(result.getDescription()).isEqualTo("January electricity");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(sharedCostRepository.findByIdAndTenantId(SHARED_COST_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sharedUtilityCostService.getSharedCost(SHARED_COST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("SharedUtilityCost");
        }
    }

    @Nested
    @DisplayName("getSharedCostsByEstate")
    class GetSharedCostsByEstate {

        @Test
        @DisplayName("should return paged shared costs")
        void shouldReturnPagedSharedCosts() {
            Pageable pageable = PageRequest.of(0, 10);
            SharedUtilityCost cost = buildSharedCost();
            SharedUtilityCostResponse response = buildSharedCostResponse();

            Page<SharedUtilityCost> page = new PageImpl<>(List.of(cost), pageable, 1);

            when(sharedCostRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(sharedCostMapper.toResponse(cost)).thenReturn(response);

            PagedResponse<SharedUtilityCostResponse> result =
                    sharedUtilityCostService.getSharedCostsByEstate(ESTATE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("generateInvoicesForSharedCost")
    class GenerateInvoicesForSharedCost {

        @Test
        @DisplayName("should generate invoices for occupied units and set invoicesGenerated to true")
        void shouldGenerateInvoicesSuccessfully() {
            SharedUtilityCost sharedCost = buildSharedCost();
            Unit unit1 = buildUnit(UNIT_ID, "A-101", 100.0);
            UUID unit2Id = UUID.randomUUID();
            Unit unit2 = buildUnit(unit2Id, "A-102", 100.0);
            Tenancy tenancy1 = buildTenancy(UNIT_ID);
            Tenancy tenancy2 = buildTenancy(unit2Id);

            when(sharedCostRepository.findByIdAndTenantId(SHARED_COST_ID, TENANT_ID))
                    .thenReturn(Optional.of(sharedCost));
            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(List.of(unit1, unit2));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(List.of(tenancy1));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(unit2Id, TENANT_ID))
                    .thenReturn(List.of(tenancy2));
            when(invoiceRepository.countByInvoiceNumberPrefix(eq(TENANT_ID), any()))
                    .thenReturn(0L);
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

            int count = sharedUtilityCostService.generateInvoicesForSharedCost(SHARED_COST_ID);

            assertThat(count).isEqualTo(2);
            assertThat(sharedCost.isInvoicesGenerated()).isTrue();
            verify(invoiceRepository, times(2)).save(any(ChargeInvoice.class));
            verify(sharedCostRepository).save(sharedCost);
            verify(notificationService, times(2)).send(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when invoices already generated")
        void shouldThrowWhenInvoicesAlreadyGenerated() {
            SharedUtilityCost sharedCost = buildSharedCost();
            sharedCost.setInvoicesGenerated(true);

            when(sharedCostRepository.findByIdAndTenantId(SHARED_COST_ID, TENANT_ID))
                    .thenReturn(Optional.of(sharedCost));

            assertThatThrownBy(() -> sharedUtilityCostService.generateInvoicesForSharedCost(SHARED_COST_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invoices have already been generated");

            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when shared cost not found")
        void shouldThrowWhenSharedCostNotFound() {
            when(sharedCostRepository.findByIdAndTenantId(SHARED_COST_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sharedUtilityCostService.generateInvoicesForSharedCost(SHARED_COST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("SharedUtilityCost");

            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip unoccupied units when generating invoices")
        void shouldSkipUnoccupiedUnits() {
            SharedUtilityCost sharedCost = buildSharedCost();
            Unit occupiedUnit = buildUnit(UNIT_ID, "A-101", 100.0);
            UUID vacantUnitId = UUID.randomUUID();
            Unit vacantUnit = buildUnit(vacantUnitId, "A-102", 100.0);
            Tenancy tenancy = buildTenancy(UNIT_ID);

            when(sharedCostRepository.findByIdAndTenantId(SHARED_COST_ID, TENANT_ID))
                    .thenReturn(Optional.of(sharedCost));
            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(List.of(occupiedUnit, vacantUnit));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(List.of(tenancy));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(vacantUnitId, TENANT_ID))
                    .thenReturn(Collections.emptyList());
            when(invoiceRepository.countByInvoiceNumberPrefix(eq(TENANT_ID), any()))
                    .thenReturn(0L);
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

            int count = sharedUtilityCostService.generateInvoicesForSharedCost(SHARED_COST_ID);

            assertThat(count).isEqualTo(1);
            verify(invoiceRepository, times(1)).save(any(ChargeInvoice.class));
            verify(notificationService, times(1)).send(any());
        }
    }
}
