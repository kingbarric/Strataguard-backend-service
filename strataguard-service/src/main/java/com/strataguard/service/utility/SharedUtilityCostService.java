package com.strataguard.service.utility;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.dto.utility.CreateSharedUtilityCostRequest;
import com.strataguard.core.dto.utility.SharedUtilityCostResponse;
import com.strataguard.core.entity.*;
import com.strataguard.core.enums.CostSplitMethod;
import com.strataguard.core.enums.InvoiceStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.SharedUtilityCostMapper;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SharedUtilityCostService {

    private final SharedUtilityCostRepository sharedCostRepository;
    private final UnitRepository unitRepository;
    private final TenancyRepository tenancyRepository;
    private final ChargeInvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final SharedUtilityCostMapper sharedCostMapper;

    public SharedUtilityCostResponse createSharedCost(CreateSharedUtilityCostRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Get occupied units in the estate
        List<Unit> allUnits = unitRepository.findByEstateIdAndTenantId(request.getEstateId(), tenantId);
        List<Unit> occupiedUnits = allUnits.stream()
                .filter(u -> !tenancyRepository.findActiveByUnitIdAndTenantId(u.getId(), tenantId).isEmpty())
                .toList();

        if (occupiedUnits.isEmpty()) {
            throw new IllegalStateException("No occupied units found in the estate");
        }

        // Compute cost per unit based on split method
        BigDecimal costPerUnit = computeCostPerUnit(request.getTotalCost(), request.getSplitMethod(), occupiedUnits);

        SharedUtilityCost sharedCost = new SharedUtilityCost();
        sharedCost.setTenantId(tenantId);
        sharedCost.setEstateId(request.getEstateId());
        sharedCost.setUtilityType(request.getUtilityType());
        sharedCost.setTotalCost(request.getTotalCost());
        sharedCost.setSplitMethod(request.getSplitMethod());
        sharedCost.setTotalUnitsParticipating(occupiedUnits.size());
        sharedCost.setCostPerUnit(costPerUnit);
        sharedCost.setBillingPeriodStart(request.getBillingPeriodStart());
        sharedCost.setBillingPeriodEnd(request.getBillingPeriodEnd());
        sharedCost.setDescription(request.getDescription());

        SharedUtilityCost saved = sharedCostRepository.save(sharedCost);
        log.info("Created shared utility cost: {} for estate: {} tenant: {}", saved.getId(), request.getEstateId(), tenantId);
        return sharedCostMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SharedUtilityCostResponse getSharedCost(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        SharedUtilityCost cost = sharedCostRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedUtilityCost", "id", id));
        return sharedCostMapper.toResponse(cost);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SharedUtilityCostResponse> getSharedCostsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<SharedUtilityCost> page = sharedCostRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return PagedResponse.<SharedUtilityCostResponse>builder()
                .content(page.getContent().stream().map(sharedCostMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    public int generateInvoicesForSharedCost(UUID sharedCostId) {
        UUID tenantId = TenantContext.requireTenantId();
        SharedUtilityCost sharedCost = sharedCostRepository.findByIdAndTenantId(sharedCostId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedUtilityCost", "id", sharedCostId));

        if (sharedCost.isInvoicesGenerated()) {
            throw new IllegalStateException("Invoices have already been generated for this shared cost");
        }

        List<Unit> allUnits = unitRepository.findByEstateIdAndTenantId(sharedCost.getEstateId(), tenantId);
        int count = 0;

        for (Unit unit : allUnits) {
            List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(unit.getId(), tenantId);
            if (activeTenancies.isEmpty()) continue;

            UUID residentId = activeTenancies.get(0).getResidentId();
            BigDecimal unitCost = computeUnitSpecificCost(sharedCost, unit, allUnits);

            ChargeInvoice invoice = new ChargeInvoice();
            invoice.setTenantId(tenantId);
            invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
            invoice.setChargeType(com.strataguard.core.enums.ChargeType.UTILITY);
            invoice.setChargeId(sharedCostId);
            invoice.setUnitId(unit.getId());
            invoice.setResidentId(residentId);
            invoice.setAmount(unitCost);
            invoice.setPenaltyAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(unitCost);
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setDueDate(LocalDate.now().plusDays(30));
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setBillingPeriodStart(sharedCost.getBillingPeriodStart());
            invoice.setBillingPeriodEnd(sharedCost.getBillingPeriodEnd());
            invoice.setNotes("Shared " + sharedCost.getUtilityType() + " cost: " +
                    (sharedCost.getDescription() != null ? sharedCost.getDescription() : ""));
            invoiceRepository.save(invoice);

            // Notify resident
            try {
                notificationService.send(SendNotificationRequest.builder()
                        .recipientId(residentId)
                        .type(NotificationType.SHARED_COST_ALLOCATED)
                        .title("Shared Utility Cost")
                        .body("A shared " + sharedCost.getUtilityType() + " cost of " + unitCost + " has been allocated to your unit")
                        .data(Map.of("invoiceAmount", unitCost.toString(), "utilityType", sharedCost.getUtilityType().name()))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to send shared cost notification: {}", e.getMessage());
            }

            count++;
        }

        sharedCost.setInvoicesGenerated(true);
        sharedCostRepository.save(sharedCost);

        log.info("Generated {} invoices for shared cost {} tenant: {}", count, sharedCostId, tenantId);
        return count;
    }

    private BigDecimal computeCostPerUnit(BigDecimal totalCost, CostSplitMethod method, List<Unit> units) {
        if (method == CostSplitMethod.EQUAL) {
            return totalCost.divide(BigDecimal.valueOf(units.size()), 2, RoundingMode.HALF_UP);
        }
        if (method == CostSplitMethod.BY_SQUARE_METERS) {
            double totalSquareMeters = units.stream()
                    .mapToDouble(u -> u.getSquareMeters() != null ? u.getSquareMeters() : 1.0)
                    .sum();
            double avgSquareMeters = totalSquareMeters / units.size();
            return totalCost.multiply(BigDecimal.valueOf(avgSquareMeters / totalSquareMeters))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // For BY_UNIT_TYPE and CUSTOM, default to equal split
        return totalCost.divide(BigDecimal.valueOf(units.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeUnitSpecificCost(SharedUtilityCost sharedCost, Unit unit, List<Unit> allUnits) {
        if (sharedCost.getSplitMethod() == CostSplitMethod.EQUAL) {
            return sharedCost.getCostPerUnit();
        }
        if (sharedCost.getSplitMethod() == CostSplitMethod.BY_SQUARE_METERS) {
            double totalSquareMeters = allUnits.stream()
                    .mapToDouble(u -> u.getSquareMeters() != null ? u.getSquareMeters() : 1.0)
                    .sum();
            double unitSquareMeters = unit.getSquareMeters() != null ? unit.getSquareMeters() : 1.0;
            return sharedCost.getTotalCost().multiply(BigDecimal.valueOf(unitSquareMeters / totalSquareMeters))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return sharedCost.getCostPerUnit();
    }

    private String generateInvoiceNumber(UUID tenantId) {
        YearMonth yearMonth = YearMonth.now();
        String prefix = String.format("SHR-%d%02d-", yearMonth.getYear(), yearMonth.getMonthValue());
        long count = invoiceRepository.countByInvoiceNumberPrefix(tenantId, prefix + "%");
        return String.format("%s%06d", prefix, count + 1);
    }
}
