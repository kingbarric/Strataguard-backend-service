package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.LevyInvoice;
import com.strataguard.core.entity.LevyType;
import com.strataguard.core.entity.Tenancy;
import com.strataguard.core.enums.InvoiceStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.InvoiceMapper;
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

    private final LevyInvoiceRepository invoiceRepository;
    private final LevyTypeRepository levyTypeRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final TenancyRepository tenancyRepository;
    private final InvoiceMapper invoiceMapper;

    @Value("${billing.penalty.rate-per-month:0.05}")
    private double penaltyRatePerMonth;

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        LevyType levyType = levyTypeRepository.findByIdAndTenantId(request.getLevyTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LevyType", "id", request.getLevyTypeId()));

        unitRepository.findByIdAndTenantId(request.getUnitId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", request.getUnitId()));

        // Find active tenancy for the unit to get the resident
        List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(request.getUnitId(), tenantId);
        UUID residentId = activeTenancies.isEmpty() ? null : activeTenancies.get(0).getResidentId();

        LevyInvoice invoice = new LevyInvoice();
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
        invoice.setLevyTypeId(request.getLevyTypeId());
        invoice.setUnitId(request.getUnitId());
        invoice.setResidentId(residentId);
        invoice.setAmount(levyType.getAmount());
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(levyType.getAmount());
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setNotes(request.getNotes());

        LevyInvoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice: {} for unit: {} tenant: {}", saved.getInvoiceNumber(), request.getUnitId(), tenantId);
        return enrichResponse(saved);
    }

    public List<InvoiceResponse> bulkGenerateInvoices(BulkInvoiceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        LevyType levyType = levyTypeRepository.findByIdAndTenantId(request.getLevyTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LevyType", "id", request.getLevyTypeId()));

        // Get all units for the estate and find active tenancies
        Page<com.strataguard.core.entity.Unit> unitsPage = unitRepository.findAllByEstateIdAndTenantId(
                request.getEstateId(), tenantId, Pageable.unpaged());

        List<InvoiceResponse> createdInvoices = new ArrayList<>();

        for (com.strataguard.core.entity.Unit unit : unitsPage.getContent()) {
            List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(unit.getId(), tenantId);
            if (activeTenancies.isEmpty()) {
                continue; // Skip units without active tenancies
            }

            Tenancy tenancy = activeTenancies.get(0);

            // Skip if an active invoice already exists for this levy type + unit + billing period
            if (request.getBillingPeriodStart() != null && request.getBillingPeriodEnd() != null) {
                if (invoiceRepository.existsActiveInvoice(request.getLevyTypeId(), unit.getId(), tenantId,
                        request.getBillingPeriodStart(), request.getBillingPeriodEnd())) {
                    log.debug("Skipping duplicate invoice for unit: {} levy: {}", unit.getId(), request.getLevyTypeId());
                    continue;
                }
            }

            LevyInvoice invoice = new LevyInvoice();
            invoice.setTenantId(tenantId);
            invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
            invoice.setLevyTypeId(request.getLevyTypeId());
            invoice.setUnitId(unit.getId());
            invoice.setResidentId(tenancy.getResidentId());
            invoice.setAmount(levyType.getAmount());
            invoice.setPenaltyAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(levyType.getAmount());
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setDueDate(request.getDueDate());
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setBillingPeriodStart(request.getBillingPeriodStart());
            invoice.setBillingPeriodEnd(request.getBillingPeriodEnd());

            LevyInvoice saved = invoiceRepository.save(invoice);
            createdInvoices.add(enrichResponse(saved));
        }

        log.info("Bulk generated {} invoices for estate: {} levy: {} tenant: {}",
                createdInvoices.size(), request.getEstateId(), request.getLevyTypeId(), tenantId);
        return createdInvoices;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return enrichResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getAllInvoices(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<LevyInvoice> page = invoiceRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getInvoicesByResident(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<LevyInvoice> page = invoiceRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getInvoicesByUnit(UUID unitId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<LevyInvoice> page = invoiceRepository.findByUnitIdAndTenantId(unitId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> getOverdueInvoices(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<LevyInvoice> page = invoiceRepository.findOverduePagedByTenantId(tenantId, pageable);
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
        Page<LevyInvoice> page = invoiceRepository.search(tenantId, query, pageable);
        return toPagedResponse(page);
    }

    public InvoiceResponse cancelInvoice(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid invoice");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setActive(false);
        LevyInvoice saved = invoiceRepository.save(invoice);
        log.info("Cancelled invoice: {} for tenant: {}", id, tenantId);
        return enrichResponse(saved);
    }

    public int applyPenalties() {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate today = LocalDate.now();

        List<LevyInvoice> overdueInvoices = invoiceRepository.findOverdueByTenantId(tenantId, today);
        int updatedCount = 0;

        for (LevyInvoice invoice : overdueInvoices) {
            long monthsOverdue = ChronoUnit.MONTHS.between(invoice.getDueDate(), today);
            if (monthsOverdue <= 0) {
                monthsOverdue = 1; // At least 1 month penalty if overdue
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

    /**
     * Update invoice payment status after a payment is recorded.
     * Called by PaymentService.
     */
    public void updateInvoicePayment(UUID invoiceId, BigDecimal paymentAmount) {
        UUID tenantId = TenantContext.requireTenantId();
        LevyInvoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
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

    private String generateInvoiceNumber(UUID tenantId) {
        YearMonth yearMonth = YearMonth.now();
        String prefix = String.format("INV-%d%02d-", yearMonth.getYear(), yearMonth.getMonthValue());
        long count = invoiceRepository.countByInvoiceNumberPrefix(tenantId, prefix + "%");
        return String.format("%s%06d", prefix, count + 1);
    }

    private InvoiceResponse enrichResponse(LevyInvoice invoice) {
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        levyTypeRepository.findByIdAndTenantId(invoice.getLevyTypeId(), invoice.getTenantId())
                .ifPresent(lt -> response.setLevyTypeName(lt.getName()));

        unitRepository.findByIdAndTenantId(invoice.getUnitId(), invoice.getTenantId())
                .ifPresent(u -> response.setUnitNumber(u.getUnitNumber()));

        if (invoice.getResidentId() != null) {
            residentRepository.findByIdAndTenantId(invoice.getResidentId(), invoice.getTenantId())
                    .ifPresent(r -> response.setResidentName(r.getFirstName() + " " + r.getLastName()));
        }

        return response;
    }

    private PagedResponse<InvoiceResponse> toPagedResponse(Page<LevyInvoice> page) {
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
