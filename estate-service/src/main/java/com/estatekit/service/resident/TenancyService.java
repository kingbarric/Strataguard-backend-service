package com.estatekit.service.resident;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.tenancy.CreateTenancyRequest;
import com.estatekit.core.dto.tenancy.TenancyResponse;
import com.estatekit.core.dto.tenancy.UpdateTenancyRequest;
import com.estatekit.core.entity.Tenancy;
import com.estatekit.core.entity.Unit;
import com.estatekit.core.enums.TenancyStatus;
import com.estatekit.core.enums.TenancyType;
import com.estatekit.core.enums.UnitStatus;
import com.estatekit.core.exception.InvalidStateTransitionException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.TenancyMapper;
import com.estatekit.infrastructure.repository.ResidentRepository;
import com.estatekit.infrastructure.repository.TenancyRepository;
import com.estatekit.infrastructure.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenancyService {

    private final TenancyRepository tenancyRepository;
    private final ResidentRepository residentRepository;
    private final UnitRepository unitRepository;
    private final TenancyMapper tenancyMapper;

    public TenancyResponse createTenancy(CreateTenancyRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify resident exists
        residentRepository.findByIdAndTenantId(request.getResidentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", request.getResidentId()));

        // Verify unit exists
        Unit unit = unitRepository.findByIdAndTenantId(request.getUnitId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", request.getUnitId()));

        // Check constraint: at most one active OWNER and one active TENANT per unit
        if (request.getTenancyType() != TenancyType.DEPENDENT) {
            if (tenancyRepository.existsActiveByUnitIdAndTenancyTypeAndTenantId(
                    request.getUnitId(), request.getTenancyType(), tenantId)) {
                throw new InvalidStateTransitionException(
                        "Unit already has an active " + request.getTenancyType() + " tenancy");
            }
        }

        Tenancy tenancy = tenancyMapper.toEntity(request);
        tenancy.setTenantId(tenantId);

        Tenancy saved = tenancyRepository.save(tenancy);

        // Set unit status to OCCUPIED
        unit.setStatus(UnitStatus.OCCUPIED);
        unitRepository.save(unit);

        log.info("Created tenancy: {} for resident: {} in unit: {} for tenant: {}",
                saved.getId(), request.getResidentId(), request.getUnitId(), tenantId);
        return tenancyMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TenancyResponse getTenancy(UUID tenancyId) {
        UUID tenantId = TenantContext.requireTenantId();
        Tenancy tenancy = tenancyRepository.findByIdAndTenantId(tenancyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenancy", "id", tenancyId));
        return tenancyMapper.toResponse(tenancy);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenancyResponse> getTenanciesByResident(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", residentId));

        Page<Tenancy> page = tenancyRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenancyResponse> getTenanciesByUnit(UUID unitId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        unitRepository.findByIdAndTenantId(unitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", unitId));

        Page<Tenancy> page = tenancyRepository.findByUnitIdAndTenantId(unitId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public TenancyResponse updateTenancy(UUID tenancyId, UpdateTenancyRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Tenancy tenancy = tenancyRepository.findByIdAndTenantId(tenancyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenancy", "id", tenancyId));

        if (tenancy.getStatus() != TenancyStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Tenancy", tenancy.getStatus().name(), "updated");
        }

        tenancyMapper.updateEntity(request, tenancy);
        Tenancy updated = tenancyRepository.save(tenancy);
        log.info("Updated tenancy: {} for tenant: {}", tenancyId, tenantId);
        return tenancyMapper.toResponse(updated);
    }

    public TenancyResponse terminateTenancy(UUID tenancyId) {
        UUID tenantId = TenantContext.requireTenantId();
        Tenancy tenancy = tenancyRepository.findByIdAndTenantId(tenancyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenancy", "id", tenancyId));

        if (tenancy.getStatus() != TenancyStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Tenancy", tenancy.getStatus().name(), "TERMINATED");
        }

        tenancy.setStatus(TenancyStatus.TERMINATED);
        tenancy.setEndDate(LocalDate.now());
        tenancy.setActive(false);
        Tenancy updated = tenancyRepository.save(tenancy);

        // If no more active tenancies on this unit, set unit status to VACANT
        List<Tenancy> activeTenancies = tenancyRepository.findActiveByUnitIdAndTenantId(tenancy.getUnitId(), tenantId);
        if (activeTenancies.isEmpty()) {
            Unit unit = unitRepository.findByIdAndTenantId(tenancy.getUnitId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", tenancy.getUnitId()));
            unit.setStatus(UnitStatus.VACANT);
            unitRepository.save(unit);
            log.info("Unit {} set to VACANT after last tenancy terminated", tenancy.getUnitId());
        }

        log.info("Terminated tenancy: {} for tenant: {}", tenancyId, tenantId);
        return tenancyMapper.toResponse(updated);
    }

    public void deleteTenancy(UUID tenancyId) {
        UUID tenantId = TenantContext.requireTenantId();
        Tenancy tenancy = tenancyRepository.findByIdAndTenantId(tenancyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenancy", "id", tenancyId));

        tenancy.setDeleted(true);
        tenancy.setActive(false);
        tenancyRepository.save(tenancy);
        log.info("Soft-deleted tenancy: {} for tenant: {}", tenancyId, tenantId);
    }

    private PagedResponse<TenancyResponse> toPagedResponse(Page<Tenancy> page) {
        return PagedResponse.<TenancyResponse>builder()
                .content(page.getContent().stream().map(tenancyMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
