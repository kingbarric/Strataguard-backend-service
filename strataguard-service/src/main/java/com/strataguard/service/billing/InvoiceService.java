package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.ChargeInvoice;
import com.strataguard.core.entity.EstateCharge;
import com.strataguard.core.entity.TenantCharge;
import com.strataguard.core.entity.Tenancy;
import com.strataguard.core.enums.ChargeType;
import com.strataguard.core.enums.InvoiceStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ChargeInvoiceMapper;
import com.strataguard.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

    private final ChargeInvoiceRepository invoiceRepository;
    private final EstateChargeRepository estateChargeRepository;
    private final TenantChargeRepository tenantChargeRepository;
    private final EstateChargeExclusionRepository exclusionRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final TenancyRepository tenancyRepository;
    private final ChargeInvoiceMapper invoiceMapper;

    @Value("${billing.penalty.rate-per-month:0.05}")
    private double penaltyRatePerMonth;

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        BigDecimal chargeAmount = lookupChargeAmount(request.getChargeId(), request.getChargeType(), tenantId);

        unitRepository.findByIdAndTenantId(request.getUnitId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", request.getUnitId()));

        // Find active tenancy for the unit to get the resident
        List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(request.getUnitId(), tenantId);
        UUID residentId = activeTenancies.isEmpty() ? null : activeTenancies.get(0).getResidentId();

        ChargeInvoice invoice = new ChargeInvoice();
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
        invoice.setChargeType(request.getChargeType());
        invoice.setChargeId(request.getChargeId());
        invoice.setUnitId(request.getUnitId());
        invoice.setResidentId(residentId);
        invoice.setAmount(chargeAmount);
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(chargeAmount);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setNotes(request.getNotes());

        ChargeInvoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice: {} for unit: {} tenant: {}", saved.getInvoiceNumber(), request.getUnitId(), tenantId);
        return enrichResponse(saved);
    }

    public List<InvoiceResponse> bulkGenerateInvoices(BulkInvoiceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        ChargeType chargeType = request.getChargeType() != null ? request.getChargeType() : ChargeType.ESTATE_CHARGE;
        BigDecimal chargeAmount = lookupChargeAmount(request.getChargeId(), chargeType, tenantId);

        // Get excluded tenancy IDs (only for estate charges)
        List<UUID> excludedTenancyIds = chargeType == ChargeType.ESTATE_CHARGE
                ? exclusionRepository.findExcludedTenancyIds(request.getChargeId(), tenantId)
                : List.of();

        // Get all units for the estate and find active tenancies
        Page<com.strataguard.core.entity.Unit> unitsPage = unitRepository.findAllByEstateIdAndTenantId(
                request.getEstateId(), tenantId, Pageable.unpaged());

        List<InvoiceResponse> createdInvoices = new ArrayList<>();

        for (com.strataguard.core.entity.Unit unit : unitsPage.getContent()) {
            List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(unit.getId(), tenantId);
            if (activeTenancies.isEmpty()) {
                continue;
            }

            Tenancy tenancy = activeTenancies.get(0);

            // Skip excluded tenancies
            if (excludedTenancyIds.contains(tenancy.getId())) {
                log.debug("Skipping excluded tenancy: {} for charge: {}", tenancy.getId(), request.getChargeId());
                continue;
            }

            // Skip if an active invoice already exists for this charge + unit + billing period
            if (request.getBillingPeriodStart() != null && request.getBillingPeriodEnd() != null) {
                if (invoiceRepository.existsActiveInvoice(request.getChargeId(), unit.getId(), tenantId,
                        request.getBillingPeriodStart(), request.getBillingPeriodEnd())) {
                    log.debug("Skipping duplicate invoice for unit: {} charge: {}", unit.getId(), request.getChargeId());
                    continue;
                }
            }

            ChargeInvoice invoice = new ChargeInvoice();
            invoice.setTenantId(tenantId);
            invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
            invoice.setChargeType(chargeType);
            invoice.setChargeId(request.getChargeId());
            invoice.setUnitId(unit.getId());
            invoice.setResidentId(tenancy.getResidentId());
            invoice.setAmount(chargeAmount);
            invoice.setPenaltyAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(chargeAmount);
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setDueDate(request.getDueDate());
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setBillingPeriodStart(request.getBillingPeriodStart());
            invoice.setBillingPeriodEnd(request.getBillingPeriodEnd());

            ChargeInvoice saved = invoiceRepository.save(invoice);
            createdInvoices.add(enrichResponse(saved));
        }

        log.info("Bulk generated {} invoices for estate: {} charge: {} tenant: {}",
                createdInvoices.size(), request.getEstateId(), request.getChargeId(), tenantId);
        return createdInvoices;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        ChargeInvoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return enrichResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getAllInvoices(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ChargeInvoice> page = invoiceRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getInvoicesByResident(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ChargeInvoice> page = invoiceRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getInvoicesByUnit(UUID unitId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ChargeInvoice> page = invoiceRepository.findByUnitIdAndTenantId(unitId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getOverdueInvoices(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ChargeInvoice> page = invoiceRepository.findOverduePagedByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public InvoiceSummaryResponse getInvoiceSummary() {
        UUID tenantId = TenantContext.requireTenantId();
        return InvoiceSummaryResponse.builder()
                .totalInvoices(invoiceRepository.countByTenantId(tenantId))
                .totalAmount(invoiceRepository.sumTotalAmountByTenantId(tenantId))
                .paidAmount(invoiceRepository.sumPaidAmountByTenantId(tenantId))
                .pendingAmount(invoiceRepository.sumPendingAmountByTenantId(tenantId))
                .overdueAmount(invoiceRepository.sumOverdueAmountByTenantId(tenantId))
                .overdueCount(invoiceRepository.countOverdueByTenantId(tenantId))
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> searchInvoices(String query, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ChargeInvoice> page = invoiceRepository.search(tenantId, query, pageable);
        return toPagedResponse(page);
    }

    public InvoiceResponse cancelInvoice(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        ChargeInvoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid invoice");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setActive(false);
        ChargeInvoice saved = invoiceRepository.save(invoice);
        log.info("Cancelled invoice: {} for tenant: {}", id, tenantId);
        return enrichResponse(saved);
    }

    public int applyPenalties() {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate today = LocalDate.now();

        List<ChargeInvoice> overdueInvoices = invoiceRepository.findOverdueByTenantId(tenantId, today);
        int updatedCount = 0;

        for (ChargeInvoice invoice : overdueInvoices) {
            long monthsOverdue = ChronoUnit.MONTHS.between(invoice.getDueDate(), today);
            if (monthsOverdue <= 0) {
                monthsOverdue = 1;
            }

            BigDecimal penaltyAmount = invoice.getAmount()
                    .multiply(BigDecimal.valueOf(penaltyRatePerMonth))
                    .multiply(BigDecimal.valueOf(monthsOverdue))
                    .setScale(2, RoundingMode.HALF_UP);

            invoice.setPenaltyAmount(penaltyAmount);
            invoice.setTotalAmount(invoice.getAmount().add(penaltyAmount));
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            updatedCount++;
        }

        log.info("Applied penalties to {} overdue invoices for tenant: {}", updatedCount, tenantId);
        return updatedCount;
    }

    public void updateInvoicePayment(UUID invoiceId, BigDecimal paymentAmount) {
        UUID tenantId = TenantContext.requireTenantId();
        ChargeInvoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(paymentAmount);
        invoice.setPaidAmount(newPaidAmount);

        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }

        invoiceRepository.save(invoice);
        log.info("Updated invoice {} payment: paidAmount={}, status={}", invoiceId, newPaidAmount, invoice.getStatus());
    }

    private BigDecimal lookupChargeAmount(UUID chargeId, ChargeType chargeType, UUID tenantId) {
        if (chargeType == ChargeType.ESTATE_CHARGE) {
            EstateCharge charge = estateChargeRepository.findByIdAndTenantId(chargeId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("EstateCharge", "id", chargeId));
            return charge.getAmount();
        } else if (chargeType == ChargeType.TENANT_CHARGE) {
            TenantCharge charge = tenantChargeRepository.findByIdAndTenantId(chargeId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("TenantCharge", "id", chargeId));
            return charge.getAmount();
        }
        throw new IllegalArgumentException("Unsupported charge type: " + chargeType);
    }

    private String generateInvoiceNumber(UUID tenantId) {
        YearMonth yearMonth = YearMonth.now();
        String prefix = String.format("INV-%d%02d-", yearMonth.getYear(), yearMonth.getMonthValue());
        long count = invoiceRepository.countByInvoiceNumberPrefix(tenantId, prefix + "%");
        return String.format("%s%06d", prefix, count + 1);
    }

    private InvoiceResponse enrichResponse(ChargeInvoice invoice) {
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        // Lookup charge name based on type
        if (invoice.getChargeType() == ChargeType.ESTATE_CHARGE) {
            estateChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId())
                    .ifPresent(c -> response.setChargeName(c.getName()));
        } else if (invoice.getChargeType() == ChargeType.TENANT_CHARGE) {
            tenantChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId())
                    .ifPresent(c -> response.setChargeName(c.getName()));
        } else {
            response.setChargeName("Utility");
        }

        unitRepository.findByIdAndTenantId(invoice.getUnitId(), invoice.getTenantId())
                .ifPresent(u -> response.setUnitNumber(u.getUnitNumber()));

        if (invoice.getResidentId() != null) {
            residentRepository.findByIdAndTenantId(invoice.getResidentId(), invoice.getTenantId())
                    .ifPresent(r -> response.setResidentName(r.getFirstName() + " " + r.getLastName()));
        }

        return response;
    }

    private PagedResponse<InvoiceResponse> toPagedResponse(Page<ChargeInvoice> page) {
        return PagedResponse.<InvoiceResponse>builder()
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
