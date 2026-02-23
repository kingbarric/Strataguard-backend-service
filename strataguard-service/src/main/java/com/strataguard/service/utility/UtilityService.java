package com.strataguard.service.utility;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.dto.utility.*;
import com.strataguard.core.entity.ChargeInvoice;
import com.strataguard.core.enums.ChargeType;
import com.strataguard.core.entity.Tenancy;
import com.strataguard.core.entity.UtilityMeter;
import com.strataguard.core.entity.UtilityReading;
import com.strataguard.core.enums.InvoiceStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.enums.UtilityReadingStatus;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.UtilityMeterMapper;
import com.strataguard.core.util.UtilityReadingMapper;
import com.strataguard.infrastructure.repository.*;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UtilityService {

    private final UtilityMeterRepository meterRepository;
    private final UtilityReadingRepository readingRepository;
    private final UnitRepository unitRepository;
    private final TenancyRepository tenancyRepository;
    private final ChargeInvoiceRepository invoiceRepository;
    private final ResidentRepository residentRepository;
    private final NotificationService notificationService;
    private final UtilityMeterMapper meterMapper;
    private final UtilityReadingMapper readingMapper;

    public UtilityMeterResponse createMeter(CreateUtilityMeterRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (meterRepository.existsByMeterNumberAndTenantId(request.getMeterNumber(), tenantId)) {
            throw new DuplicateResourceException("UtilityMeter", "meterNumber", request.getMeterNumber());
        }

        UtilityMeter meter = meterMapper.toEntity(request);
        meter.setTenantId(tenantId);

        UtilityMeter saved = meterRepository.save(meter);
        log.info("Created utility meter: {} for tenant: {}", saved.getMeterNumber(), tenantId);
        return enrichMeterResponse(saved);
    }

    public UtilityMeterResponse updateMeter(UUID meterId, UpdateUtilityMeterRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UtilityMeter meter = meterRepository.findByIdAndTenantId(meterId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityMeter", "id", meterId));

        if (request.getMeterType() != null) meter.setMeterType(request.getMeterType());
        if (request.getRatePerUnit() != null) meter.setRatePerUnit(request.getRatePerUnit());
        if (request.getUnitOfMeasure() != null) meter.setUnitOfMeasure(request.getUnitOfMeasure());
        if (request.getConsumptionAlertThreshold() != null) meter.setConsumptionAlertThreshold(request.getConsumptionAlertThreshold());

        UtilityMeter saved = meterRepository.save(meter);
        log.info("Updated utility meter: {} for tenant: {}", meterId, tenantId);
        return enrichMeterResponse(saved);
    }

    @Transactional(readOnly = true)
    public UtilityMeterResponse getMeter(UUID meterId) {
        UUID tenantId = TenantContext.requireTenantId();
        UtilityMeter meter = meterRepository.findByIdAndTenantId(meterId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityMeter", "id", meterId));
        return enrichMeterResponse(meter);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UtilityMeterResponse> getAllMeters(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<UtilityMeter> page = meterRepository.findAllByTenantId(tenantId, pageable);
        return toMeterPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UtilityMeterResponse> getMetersByUnit(UUID unitId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<UtilityMeter> page = meterRepository.findByUnitIdAndTenantId(unitId, tenantId, pageable);
        return toMeterPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UtilityMeterResponse> getMetersByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<UtilityMeter> page = meterRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toMeterPagedResponse(page);
    }

    public UtilityReadingResponse recordReading(RecordReadingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        UtilityMeter meter = meterRepository.findByIdAndTenantId(request.getMeterId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityMeter", "id", request.getMeterId()));

        double previousReading = meter.getLastReadingValue() != null ? meter.getLastReadingValue() : 0.0;

        if (request.getCurrentReading() < previousReading) {
            throw new IllegalArgumentException("Current reading cannot be less than previous reading (" + previousReading + ")");
        }

        double consumption = request.getCurrentReading() - previousReading;
        BigDecimal cost = null;
        if (meter.getRatePerUnit() != null) {
            cost = meter.getRatePerUnit().multiply(BigDecimal.valueOf(consumption)).setScale(2, RoundingMode.HALF_UP);
        }

        UtilityReading reading = new UtilityReading();
        reading.setTenantId(tenantId);
        reading.setMeterId(meter.getId());
        reading.setUnitId(meter.getUnitId());
        reading.setUtilityType(meter.getUtilityType());
        reading.setPreviousReading(previousReading);
        reading.setCurrentReading(request.getCurrentReading());
        reading.setConsumption(consumption);
        reading.setRatePerUnit(meter.getRatePerUnit());
        reading.setCost(cost);
        reading.setBillingPeriodStart(request.getBillingPeriodStart());
        reading.setBillingPeriodEnd(request.getBillingPeriodEnd());
        reading.setReadingDate(request.getReadingDate());
        reading.setStatus(UtilityReadingStatus.PENDING);
        reading.setNotes(request.getNotes());

        UtilityReading saved = readingRepository.save(reading);

        // Update meter's last reading
        meter.setLastReadingValue(request.getCurrentReading());
        meter.setLastReadingDate(request.getReadingDate());
        meterRepository.save(meter);

        log.info("Recorded utility reading for meter {}: consumption={}", meter.getMeterNumber(), consumption);

        // Check consumption alert threshold
        if (meter.getConsumptionAlertThreshold() != null && consumption > meter.getConsumptionAlertThreshold()) {
            sendConsumptionAlert(meter, consumption, tenantId);
        }

        // Send reading recorded notification
        if (meter.getUnitId() != null) {
            sendReadingNotification(meter, consumption, tenantId);
        }

        return enrichReadingResponse(saved, meter.getMeterNumber());
    }

    public UtilityReadingResponse validateReading(UUID readingId) {
        UUID tenantId = TenantContext.requireTenantId();
        UtilityReading reading = readingRepository.findByIdAndTenantId(readingId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityReading", "id", readingId));

        if (reading.getStatus() != UtilityReadingStatus.PENDING) {
            throw new IllegalStateException("Reading is not in PENDING status");
        }

        reading.setStatus(UtilityReadingStatus.VALIDATED);
        UtilityReading saved = readingRepository.save(reading);
        log.info("Validated utility reading: {}", readingId);
        return enrichReadingResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UtilityReadingResponse> getReadingsByMeter(UUID meterId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<UtilityReading> page = readingRepository.findByMeterIdAndTenantId(meterId, tenantId, pageable);
        return toReadingPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public List<UtilityReadingResponse> getReadingsByUnitAndPeriod(UUID unitId, LocalDate periodStart, LocalDate periodEnd) {
        UUID tenantId = TenantContext.requireTenantId();
        List<UtilityReading> readings = readingRepository.findByUnitIdAndPeriodAndTenantId(unitId, periodStart, periodEnd, tenantId);
        return readings.stream().map(r -> enrichReadingResponse(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public ConsumptionTrendResponse getConsumptionTrend(UUID meterId, int months) {
        UUID tenantId = TenantContext.requireTenantId();
        UtilityMeter meter = meterRepository.findByIdAndTenantId(meterId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityMeter", "id", meterId));

        LocalDate sinceDate = LocalDate.now().minusMonths(months);
        List<UtilityReading> readings = readingRepository.findByMeterIdSinceDateAndTenantId(meterId, sinceDate, tenantId);

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<UtilityReading>> byMonth = readings.stream()
                .collect(Collectors.groupingBy(r -> r.getReadingDate().format(monthFormatter)));

        List<ConsumptionTrendResponse.MonthlyConsumption> monthlyData = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    double totalConsumption = entry.getValue().stream()
                            .mapToDouble(UtilityReading::getConsumption).sum();
                    BigDecimal totalCost = entry.getValue().stream()
                            .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return ConsumptionTrendResponse.MonthlyConsumption.builder()
                            .month(entry.getKey())
                            .consumption(totalConsumption)
                            .cost(totalCost)
                            .build();
                }).toList();

        return ConsumptionTrendResponse.builder()
                .meterId(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .utilityType(meter.getUtilityType())
                .monthlyData(monthlyData)
                .build();
    }

    @Transactional(readOnly = true)
    public UtilityStatementResponse getUtilityStatement(UUID unitId, LocalDate periodStart, LocalDate periodEnd) {
        UUID tenantId = TenantContext.requireTenantId();
        var unit = unitRepository.findByIdAndTenantId(unitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", unitId));

        List<UtilityReading> readings = readingRepository.findByUnitIdAndPeriodAndTenantId(unitId, periodStart, periodEnd, tenantId);

        BigDecimal totalCost = readings.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UtilityReadingResponse> readingResponses = readings.stream()
                .map(r -> enrichReadingResponse(r, null)).toList();

        return UtilityStatementResponse.builder()
                .unitId(unitId)
                .unitNumber(unit.getUnitNumber())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .readings(readingResponses)
                .totalCost(totalCost)
                .build();
    }

    public int generateInvoicesFromReadings() {
        UUID tenantId = TenantContext.requireTenantId();
        List<UtilityReading> validatedReadings = readingRepository.findUninvoicedValidatedReadings(tenantId);
        int count = 0;

        for (UtilityReading reading : validatedReadings) {
            if (reading.getUnitId() == null || reading.getCost() == null || reading.getCost().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Find active tenancy for the unit
            List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(reading.getUnitId(), tenantId);
            if (activeTenancies.isEmpty()) continue;

            UUID residentId = activeTenancies.get(0).getResidentId();

            ChargeInvoice invoice = new ChargeInvoice();
            invoice.setTenantId(tenantId);
            invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
            invoice.setChargeType(ChargeType.UTILITY);
            invoice.setChargeId(reading.getMeterId());
            invoice.setUnitId(reading.getUnitId());
            invoice.setResidentId(residentId);
            invoice.setAmount(reading.getCost());
            invoice.setPenaltyAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(reading.getCost());
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setDueDate(LocalDate.now().plusDays(30));
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setBillingPeriodStart(reading.getBillingPeriodStart());
            invoice.setBillingPeriodEnd(reading.getBillingPeriodEnd());
            invoice.setNotes("Utility invoice for " + reading.getUtilityType() + " consumption: " + reading.getConsumption());
            ChargeInvoice savedInvoice = invoiceRepository.save(invoice);

            reading.setInvoiceId(savedInvoice.getId());
            reading.setStatus(UtilityReadingStatus.INVOICED);
            readingRepository.save(reading);

            // Notify resident
            try {
                notificationService.send(SendNotificationRequest.builder()
                        .recipientId(residentId)
                        .type(NotificationType.UTILITY_INVOICE_GENERATED)
                        .title("Utility Invoice Generated")
                        .body("A utility invoice for " + reading.getCost() + " has been generated for " + reading.getUtilityType())
                        .data(Map.of("invoiceId", savedInvoice.getId().toString()))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to send utility invoice notification: {}", e.getMessage());
            }

            count++;
        }

        log.info("Generated {} utility invoices for tenant: {}", count, tenantId);
        return count;
    }

    @Transactional(readOnly = true)
    public UtilityDashboardResponse getDashboard() {
        UUID tenantId = TenantContext.requireTenantId();
        return UtilityDashboardResponse.builder()
                .totalMeters(meterRepository.countByTenantId(tenantId))
                .activeMeters(meterRepository.countActiveByTenantId(tenantId))
                .pendingReadings(readingRepository.countByStatusAndTenantId(UtilityReadingStatus.PENDING, tenantId))
                .validatedReadings(readingRepository.countByStatusAndTenantId(UtilityReadingStatus.VALIDATED, tenantId))
                .totalUtilityCosts(readingRepository.sumCostByTenantId(tenantId))
                .sharedCostsCount(0) // Populated separately if needed
                .build();
    }

    private void sendConsumptionAlert(UtilityMeter meter, double consumption, UUID tenantId) {
        if (meter.getUnitId() == null) return;
        List<Tenancy> tenancies = tenancyRepository.findActiveByUnitIdAndTenantId(meter.getUnitId(), tenantId);
        for (Tenancy tenancy : tenancies) {
            try {
                notificationService.send(SendNotificationRequest.builder()
                        .recipientId(tenancy.getResidentId())
                        .type(NotificationType.UTILITY_CONSUMPTION_ALERT)
                        .title("High Consumption Alert")
                        .body("Your " + meter.getUtilityType() + " consumption (" + consumption + ") exceeds the threshold (" + meter.getConsumptionAlertThreshold() + ")")
                        .data(Map.of("meterId", meter.getId().toString(), "consumption", String.valueOf(consumption)))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to send consumption alert: {}", e.getMessage());
            }
        }
    }

    private void sendReadingNotification(UtilityMeter meter, double consumption, UUID tenantId) {
        List<Tenancy> tenancies = tenancyRepository.findActiveByUnitIdAndTenantId(meter.getUnitId(), tenantId);
        for (Tenancy tenancy : tenancies) {
            try {
                notificationService.send(SendNotificationRequest.builder()
                        .recipientId(tenancy.getResidentId())
                        .type(NotificationType.UTILITY_READING_RECORDED)
                        .title("Utility Reading Recorded")
                        .body("A new " + meter.getUtilityType() + " reading has been recorded. Consumption: " + consumption)
                        .data(Map.of("meterId", meter.getId().toString()))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to send reading notification: {}", e.getMessage());
            }
        }
    }

    private String generateInvoiceNumber(UUID tenantId) {
        YearMonth yearMonth = YearMonth.now();
        String prefix = String.format("UTL-%d%02d-", yearMonth.getYear(), yearMonth.getMonthValue());
        long count = invoiceRepository.countByInvoiceNumberPrefix(tenantId, prefix + "%");
        return String.format("%s%06d", prefix, count + 1);
    }

    private UtilityMeterResponse enrichMeterResponse(UtilityMeter meter) {
        UUID tenantId = TenantContext.requireTenantId();
        UtilityMeterResponse response = meterMapper.toResponse(meter);
        if (meter.getUnitId() != null) {
            unitRepository.findByIdAndTenantId(meter.getUnitId(), tenantId)
                    .ifPresent(u -> response.setUnitNumber(u.getUnitNumber()));
        }
        return response;
    }

    private UtilityReadingResponse enrichReadingResponse(UtilityReading reading, String meterNumber) {
        UtilityReadingResponse response = readingMapper.toResponse(reading);
        if (meterNumber != null) {
            response.setMeterNumber(meterNumber);
        } else {
            meterRepository.findByIdAndTenantId(reading.getMeterId(), TenantContext.requireTenantId())
                    .ifPresent(m -> response.setMeterNumber(m.getMeterNumber()));
        }
        return response;
    }

    private PagedResponse<UtilityMeterResponse> toMeterPagedResponse(Page<UtilityMeter> page) {
        return PagedResponse.<UtilityMeterResponse>builder()
                .content(page.getContent().stream().map(this::enrichMeterResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    private PagedResponse<UtilityReadingResponse> toReadingPagedResponse(Page<UtilityReading> page) {
        return PagedResponse.<UtilityReadingResponse>builder()
                .content(page.getContent().stream().map(r -> enrichReadingResponse(r, null)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
