package com.strataguard.service.utility;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.utility.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.*;
import com.strataguard.core.enums.MeterType;
import com.strataguard.core.enums.UtilityReadingStatus;
import com.strataguard.core.enums.UtilityType;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.UtilityMeterMapper;
import com.strataguard.core.util.UtilityReadingMapper;
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
class UtilityServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID METER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID READING_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private UtilityMeterRepository meterRepository;

    @Mock
    private UtilityReadingRepository readingRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private TenancyRepository tenancyRepository;

    @Mock
    private ChargeInvoiceRepository invoiceRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UtilityMeterMapper meterMapper;

    @Mock
    private UtilityReadingMapper readingMapper;

    @InjectMocks
    private UtilityService utilityService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private UtilityMeter buildMeter() {
        UtilityMeter meter = new UtilityMeter();
        meter.setId(METER_ID);
        meter.setTenantId(TENANT_ID);
        meter.setMeterNumber("MTR-001");
        meter.setUnitId(UNIT_ID);
        meter.setEstateId(ESTATE_ID);
        meter.setUtilityType(UtilityType.ELECTRICITY);
        meter.setMeterType(MeterType.POSTPAID);
        meter.setRatePerUnit(new BigDecimal("0.1500"));
        meter.setUnitOfMeasure("kWh");
        meter.setConsumptionAlertThreshold(500.0);
        meter.setLastReadingValue(100.0);
        meter.setLastReadingDate(LocalDate.of(2026, 1, 1));
        meter.setActive(true);
        return meter;
    }

    private UtilityReading buildReading() {
        UtilityReading reading = new UtilityReading();
        reading.setId(READING_ID);
        reading.setTenantId(TENANT_ID);
        reading.setMeterId(METER_ID);
        reading.setUnitId(UNIT_ID);
        reading.setUtilityType(UtilityType.ELECTRICITY);
        reading.setPreviousReading(100.0);
        reading.setCurrentReading(250.0);
        reading.setConsumption(150.0);
        reading.setRatePerUnit(new BigDecimal("0.1500"));
        reading.setCost(new BigDecimal("22.50"));
        reading.setBillingPeriodStart(LocalDate.of(2026, 1, 1));
        reading.setBillingPeriodEnd(LocalDate.of(2026, 1, 31));
        reading.setReadingDate(LocalDate.of(2026, 1, 31));
        reading.setStatus(UtilityReadingStatus.PENDING);
        reading.setNotes("January reading");
        return reading;
    }

    private Unit buildUnit() {
        Unit unit = new Unit();
        unit.setId(UNIT_ID);
        unit.setTenantId(TENANT_ID);
        unit.setUnitNumber("A-101");
        unit.setEstateId(ESTATE_ID);
        return unit;
    }

    private Tenancy buildTenancy() {
        Tenancy tenancy = new Tenancy();
        tenancy.setId(UUID.randomUUID());
        tenancy.setTenantId(TENANT_ID);
        tenancy.setResidentId(RESIDENT_ID);
        tenancy.setUnitId(UNIT_ID);
        return tenancy;
    }

    private CreateUtilityMeterRequest buildCreateMeterRequest() {
        return CreateUtilityMeterRequest.builder()
                .meterNumber("MTR-001")
                .unitId(UNIT_ID)
                .estateId(ESTATE_ID)
                .utilityType(UtilityType.ELECTRICITY)
                .meterType(MeterType.POSTPAID)
                .ratePerUnit(new BigDecimal("0.1500"))
                .unitOfMeasure("kWh")
                .consumptionAlertThreshold(500.0)
                .build();
    }

    private UpdateUtilityMeterRequest buildUpdateMeterRequest() {
        return UpdateUtilityMeterRequest.builder()
                .meterType(MeterType.PREPAID)
                .ratePerUnit(new BigDecimal("0.2000"))
                .unitOfMeasure("kWh")
                .consumptionAlertThreshold(600.0)
                .build();
    }

    private RecordReadingRequest buildRecordReadingRequest() {
        return RecordReadingRequest.builder()
                .meterId(METER_ID)
                .currentReading(250.0)
                .readingDate(LocalDate.of(2026, 1, 31))
                .billingPeriodStart(LocalDate.of(2026, 1, 1))
                .billingPeriodEnd(LocalDate.of(2026, 1, 31))
                .notes("January reading")
                .build();
    }

    private UtilityMeterResponse buildMeterResponse() {
        return UtilityMeterResponse.builder()
                .id(METER_ID)
                .meterNumber("MTR-001")
                .unitId(UNIT_ID)
                .unitNumber("A-101")
                .estateId(ESTATE_ID)
                .utilityType(UtilityType.ELECTRICITY)
                .meterType(MeterType.POSTPAID)
                .ratePerUnit(new BigDecimal("0.1500"))
                .unitOfMeasure("kWh")
                .consumptionAlertThreshold(500.0)
                .lastReadingValue(100.0)
                .lastReadingDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();
    }

    private UtilityReadingResponse buildReadingResponse() {
        return UtilityReadingResponse.builder()
                .id(READING_ID)
                .meterId(METER_ID)
                .meterNumber("MTR-001")
                .unitId(UNIT_ID)
                .utilityType(UtilityType.ELECTRICITY)
                .previousReading(100.0)
                .currentReading(250.0)
                .consumption(150.0)
                .ratePerUnit(new BigDecimal("0.1500"))
                .cost(new BigDecimal("22.50"))
                .billingPeriodStart(LocalDate.of(2026, 1, 1))
                .billingPeriodEnd(LocalDate.of(2026, 1, 31))
                .readingDate(LocalDate.of(2026, 1, 31))
                .status(UtilityReadingStatus.PENDING)
                .notes("January reading")
                .build();
    }

    private void stubEnrichMeterResponse(UtilityMeter meter) {
        UtilityMeterResponse response = buildMeterResponse();
        when(meterMapper.toResponse(meter)).thenReturn(response);
        when(unitRepository.findByIdAndTenantId(meter.getUnitId(), TENANT_ID))
                .thenReturn(Optional.of(buildUnit()));
    }

    private void stubEnrichReadingResponse(UtilityReading reading, String meterNumber) {
        UtilityReadingResponse response = buildReadingResponse();
        when(readingMapper.toResponse(reading)).thenReturn(response);
        // If meterNumber is null, the service will look it up from the repository
        if (meterNumber == null) {
            when(meterRepository.findByIdAndTenantId(reading.getMeterId(), TENANT_ID))
                    .thenReturn(Optional.of(buildMeter()));
        }
    }

    // -------------------------------------------------------------------------
    // Test groups
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createMeter")
    class CreateMeter {

        @Test
        @DisplayName("should create meter successfully")
        void shouldCreateMeterSuccessfully() {
            CreateUtilityMeterRequest request = buildCreateMeterRequest();
            UtilityMeter meter = buildMeter();

            when(meterRepository.existsByMeterNumberAndTenantId("MTR-001", TENANT_ID)).thenReturn(false);
            when(meterMapper.toEntity(request)).thenReturn(meter);
            when(meterRepository.save(any(UtilityMeter.class))).thenReturn(meter);
            stubEnrichMeterResponse(meter);

            UtilityMeterResponse result = utilityService.createMeter(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(METER_ID);
            assertThat(result.getMeterNumber()).isEqualTo("MTR-001");
            assertThat(result.getUtilityType()).isEqualTo(UtilityType.ELECTRICITY);
            assertThat(result.getUnitNumber()).isEqualTo("A-101");
            verify(meterRepository).save(any(UtilityMeter.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when meter number already exists")
        void shouldThrowWhenMeterNumberDuplicate() {
            CreateUtilityMeterRequest request = buildCreateMeterRequest();

            when(meterRepository.existsByMeterNumberAndTenantId("MTR-001", TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> utilityService.createMeter(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("meterNumber");

            verify(meterRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateMeter")
    class UpdateMeter {

        @Test
        @DisplayName("should update meter successfully")
        void shouldUpdateMeterSuccessfully() {
            UpdateUtilityMeterRequest request = buildUpdateMeterRequest();
            UtilityMeter meter = buildMeter();
            UtilityMeter updatedMeter = buildMeter();
            updatedMeter.setMeterType(MeterType.PREPAID);
            updatedMeter.setRatePerUnit(new BigDecimal("0.2000"));

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.of(meter));
            when(meterRepository.save(any(UtilityMeter.class))).thenReturn(updatedMeter);
            stubEnrichMeterResponse(updatedMeter);

            UtilityMeterResponse result = utilityService.updateMeter(METER_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(METER_ID);
            verify(meterRepository).save(any(UtilityMeter.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when meter not found")
        void shouldThrowWhenMeterNotFound() {
            UpdateUtilityMeterRequest request = buildUpdateMeterRequest();

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilityService.updateMeter(METER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UtilityMeter");

            verify(meterRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMeter")
    class GetMeter {

        @Test
        @DisplayName("should return meter when found")
        void shouldReturnMeterWhenFound() {
            UtilityMeter meter = buildMeter();

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.of(meter));
            stubEnrichMeterResponse(meter);

            UtilityMeterResponse result = utilityService.getMeter(METER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(METER_ID);
            assertThat(result.getMeterNumber()).isEqualTo("MTR-001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when meter not found")
        void shouldThrowWhenMeterNotFound() {
            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilityService.getMeter(METER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UtilityMeter");
        }
    }

    @Nested
    @DisplayName("getMetersByUnit")
    class GetMetersByUnit {

        @Test
        @DisplayName("should return paged meters by unit")
        void shouldReturnPagedMetersByUnit() {
            Pageable pageable = PageRequest.of(0, 10);
            UtilityMeter meter = buildMeter();
            Page<UtilityMeter> page = new PageImpl<>(List.of(meter), pageable, 1);

            when(meterRepository.findByUnitIdAndTenantId(UNIT_ID, TENANT_ID, pageable)).thenReturn(page);
            stubEnrichMeterResponse(meter);

            PagedResponse<UtilityMeterResponse> result = utilityService.getMetersByUnit(UNIT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("getMetersByEstate")
    class GetMetersByEstate {

        @Test
        @DisplayName("should return paged meters by estate")
        void shouldReturnPagedMetersByEstate() {
            Pageable pageable = PageRequest.of(0, 10);
            UtilityMeter meter = buildMeter();
            Page<UtilityMeter> page = new PageImpl<>(List.of(meter), pageable, 1);

            when(meterRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable)).thenReturn(page);
            stubEnrichMeterResponse(meter);

            PagedResponse<UtilityMeterResponse> result = utilityService.getMetersByEstate(ESTATE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("recordReading")
    class RecordReading {

        @Test
        @DisplayName("should record reading and compute consumption correctly")
        void shouldRecordReadingSuccessfully() {
            RecordReadingRequest request = buildRecordReadingRequest();
            UtilityMeter meter = buildMeter();
            meter.setLastReadingValue(100.0);
            UtilityReading savedReading = buildReading();
            Tenancy tenancy = buildTenancy();

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.of(meter));
            when(readingRepository.save(any(UtilityReading.class))).thenReturn(savedReading);
            when(meterRepository.save(any(UtilityMeter.class))).thenReturn(meter);
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(List.of(tenancy));

            UtilityReadingResponse response = buildReadingResponse();
            when(readingMapper.toResponse(savedReading)).thenReturn(response);

            UtilityReadingResponse result = utilityService.recordReading(request);

            assertThat(result).isNotNull();
            assertThat(result.getConsumption()).isEqualTo(150.0);
            assertThat(result.getCost()).isEqualByComparingTo(new BigDecimal("22.50"));
            verify(readingRepository).save(any(UtilityReading.class));
            verify(meterRepository).save(meter);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when current reading less than previous")
        void shouldThrowWhenCurrentReadingLessThanPrevious() {
            RecordReadingRequest request = RecordReadingRequest.builder()
                    .meterId(METER_ID)
                    .currentReading(50.0)
                    .readingDate(LocalDate.of(2026, 1, 31))
                    .build();
            UtilityMeter meter = buildMeter();
            meter.setLastReadingValue(100.0);

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.of(meter));

            assertThatThrownBy(() -> utilityService.recordReading(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Current reading cannot be less than previous reading");

            verify(readingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should trigger consumption alert when threshold exceeded")
        void shouldTriggerConsumptionAlertWhenThresholdExceeded() {
            UtilityMeter meter = buildMeter();
            meter.setLastReadingValue(100.0);
            meter.setConsumptionAlertThreshold(100.0); // threshold = 100, consumption will be 550

            RecordReadingRequest request = RecordReadingRequest.builder()
                    .meterId(METER_ID)
                    .currentReading(650.0)
                    .readingDate(LocalDate.of(2026, 1, 31))
                    .billingPeriodStart(LocalDate.of(2026, 1, 1))
                    .billingPeriodEnd(LocalDate.of(2026, 1, 31))
                    .build();

            UtilityReading savedReading = buildReading();
            savedReading.setCurrentReading(650.0);
            savedReading.setConsumption(550.0);
            Tenancy tenancy = buildTenancy();

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.of(meter));
            when(readingRepository.save(any(UtilityReading.class))).thenReturn(savedReading);
            when(meterRepository.save(any(UtilityMeter.class))).thenReturn(meter);
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(List.of(tenancy));

            UtilityReadingResponse response = buildReadingResponse();
            response.setConsumption(550.0);
            when(readingMapper.toResponse(savedReading)).thenReturn(response);

            utilityService.recordReading(request);

            // Consumption alert + reading notification = 2 notification calls
            verify(notificationService, times(2)).send(any());
        }
    }

    @Nested
    @DisplayName("validateReading")
    class ValidateReading {

        @Test
        @DisplayName("should validate reading successfully")
        void shouldValidateReadingSuccessfully() {
            UtilityReading reading = buildReading();
            reading.setStatus(UtilityReadingStatus.PENDING);
            UtilityReading validatedReading = buildReading();
            validatedReading.setStatus(UtilityReadingStatus.VALIDATED);

            when(readingRepository.findByIdAndTenantId(READING_ID, TENANT_ID)).thenReturn(Optional.of(reading));
            when(readingRepository.save(any(UtilityReading.class))).thenReturn(validatedReading);
            stubEnrichReadingResponse(validatedReading, null);

            UtilityReadingResponse result = utilityService.validateReading(READING_ID);

            assertThat(result).isNotNull();
            assertThat(reading.getStatus()).isEqualTo(UtilityReadingStatus.VALIDATED);
            verify(readingRepository).save(any(UtilityReading.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when reading is not PENDING")
        void shouldThrowWhenReadingNotPending() {
            UtilityReading reading = buildReading();
            reading.setStatus(UtilityReadingStatus.VALIDATED);

            when(readingRepository.findByIdAndTenantId(READING_ID, TENANT_ID)).thenReturn(Optional.of(reading));

            assertThatThrownBy(() -> utilityService.validateReading(READING_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Reading is not in PENDING status");

            verify(readingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getReadingsByMeter")
    class GetReadingsByMeter {

        @Test
        @DisplayName("should return paged readings by meter")
        void shouldReturnPagedReadingsByMeter() {
            Pageable pageable = PageRequest.of(0, 10);
            UtilityReading reading = buildReading();
            Page<UtilityReading> page = new PageImpl<>(List.of(reading), pageable, 1);
            UtilityMeter meter = buildMeter();

            when(readingRepository.findByMeterIdAndTenantId(METER_ID, TENANT_ID, pageable)).thenReturn(page);
            when(readingMapper.toResponse(reading)).thenReturn(buildReadingResponse());
            when(meterRepository.findByIdAndTenantId(reading.getMeterId(), TENANT_ID)).thenReturn(Optional.of(meter));

            PagedResponse<UtilityReadingResponse> result = utilityService.getReadingsByMeter(METER_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("getConsumptionTrend")
    class GetConsumptionTrend {

        @Test
        @DisplayName("should return consumption trend data grouped by month")
        void shouldReturnConsumptionTrendData() {
            UtilityMeter meter = buildMeter();
            UtilityReading janReading = buildReading();
            janReading.setReadingDate(LocalDate.of(2026, 1, 15));
            janReading.setConsumption(150.0);
            janReading.setCost(new BigDecimal("22.50"));

            when(meterRepository.findByIdAndTenantId(METER_ID, TENANT_ID)).thenReturn(Optional.of(meter));
            when(readingRepository.findByMeterIdSinceDateAndTenantId(eq(METER_ID), any(LocalDate.class), eq(TENANT_ID)))
                    .thenReturn(List.of(janReading));

            ConsumptionTrendResponse result = utilityService.getConsumptionTrend(METER_ID, 6);

            assertThat(result).isNotNull();
            assertThat(result.getMeterId()).isEqualTo(METER_ID);
            assertThat(result.getMeterNumber()).isEqualTo("MTR-001");
            assertThat(result.getUtilityType()).isEqualTo(UtilityType.ELECTRICITY);
            assertThat(result.getMonthlyData()).hasSize(1);
            assertThat(result.getMonthlyData().get(0).getMonth()).isEqualTo("2026-01");
            assertThat(result.getMonthlyData().get(0).getConsumption()).isEqualTo(150.0);
            assertThat(result.getMonthlyData().get(0).getCost()).isEqualByComparingTo(new BigDecimal("22.50"));
        }
    }

    @Nested
    @DisplayName("getUtilityStatement")
    class GetUtilityStatement {

        @Test
        @DisplayName("should return utility statement for unit within period")
        void shouldReturnUtilityStatement() {
            Unit unit = buildUnit();
            LocalDate periodStart = LocalDate.of(2026, 1, 1);
            LocalDate periodEnd = LocalDate.of(2026, 1, 31);
            UtilityReading reading = buildReading();
            UtilityMeter meter = buildMeter();

            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(Optional.of(unit));
            when(readingRepository.findByUnitIdAndPeriodAndTenantId(UNIT_ID, periodStart, periodEnd, TENANT_ID))
                    .thenReturn(List.of(reading));
            when(readingMapper.toResponse(reading)).thenReturn(buildReadingResponse());
            when(meterRepository.findByIdAndTenantId(reading.getMeterId(), TENANT_ID)).thenReturn(Optional.of(meter));

            UtilityStatementResponse result = utilityService.getUtilityStatement(UNIT_ID, periodStart, periodEnd);

            assertThat(result).isNotNull();
            assertThat(result.getUnitId()).isEqualTo(UNIT_ID);
            assertThat(result.getUnitNumber()).isEqualTo("A-101");
            assertThat(result.getPeriodStart()).isEqualTo(periodStart);
            assertThat(result.getPeriodEnd()).isEqualTo(periodEnd);
            assertThat(result.getReadings()).hasSize(1);
            assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("22.50"));
        }
    }

    @Nested
    @DisplayName("generateInvoicesFromReadings")
    class GenerateInvoicesFromReadings {

        @Test
        @DisplayName("should create invoices and mark readings as INVOICED")
        void shouldCreateInvoicesAndUpdateReadings() {
            UtilityReading reading = buildReading();
            reading.setStatus(UtilityReadingStatus.VALIDATED);
            reading.setCost(new BigDecimal("22.50"));
            Tenancy tenancy = buildTenancy();
            ChargeInvoice savedInvoice = new ChargeInvoice();
            savedInvoice.setId(UUID.randomUUID());

            when(readingRepository.findUninvoicedValidatedReadings(TENANT_ID)).thenReturn(List.of(reading));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID)).thenReturn(List.of(tenancy));
            when(invoiceRepository.countByInvoiceNumberPrefix(eq(TENANT_ID), any())).thenReturn(0L);
            when(invoiceRepository.save(any(ChargeInvoice.class))).thenReturn(savedInvoice);
            when(readingRepository.save(any(UtilityReading.class))).thenReturn(reading);

            int count = utilityService.generateInvoicesFromReadings();

            assertThat(count).isEqualTo(1);
            assertThat(reading.getStatus()).isEqualTo(UtilityReadingStatus.INVOICED);
            assertThat(reading.getInvoiceId()).isEqualTo(savedInvoice.getId());
            verify(invoiceRepository).save(any(ChargeInvoice.class));
            verify(readingRepository).save(reading);
            verify(notificationService).send(any());
        }

        @Test
        @DisplayName("should skip readings with no unit or zero cost")
        void shouldSkipReadingsWithNoUnitOrZeroCost() {
            UtilityReading readingNoUnit = buildReading();
            readingNoUnit.setStatus(UtilityReadingStatus.VALIDATED);
            readingNoUnit.setUnitId(null);

            UtilityReading readingZeroCost = buildReading();
            readingZeroCost.setStatus(UtilityReadingStatus.VALIDATED);
            readingZeroCost.setCost(BigDecimal.ZERO);

            when(readingRepository.findUninvoicedValidatedReadings(TENANT_ID))
                    .thenReturn(List.of(readingNoUnit, readingZeroCost));

            int count = utilityService.generateInvoicesFromReadings();

            assertThat(count).isZero();
            verify(invoiceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return dashboard with all counts")
        void shouldReturnDashboardWithCounts() {
            when(meterRepository.countByTenantId(TENANT_ID)).thenReturn(10L);
            when(meterRepository.countActiveByTenantId(TENANT_ID)).thenReturn(8L);
            when(readingRepository.countByStatusAndTenantId(UtilityReadingStatus.PENDING, TENANT_ID)).thenReturn(5L);
            when(readingRepository.countByStatusAndTenantId(UtilityReadingStatus.VALIDATED, TENANT_ID)).thenReturn(3L);
            when(readingRepository.sumCostByTenantId(TENANT_ID)).thenReturn(new BigDecimal("12500.00"));

            UtilityDashboardResponse result = utilityService.getDashboard();

            assertThat(result).isNotNull();
            assertThat(result.getTotalMeters()).isEqualTo(10L);
            assertThat(result.getActiveMeters()).isEqualTo(8L);
            assertThat(result.getPendingReadings()).isEqualTo(5L);
            assertThat(result.getValidatedReadings()).isEqualTo(3L);
            assertThat(result.getTotalUtilityCosts()).isEqualByComparingTo(new BigDecimal("12500.00"));
            assertThat(result.getSharedCostsCount()).isZero();
        }
    }
}
