package com.strataguard.service.estate;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.unit.CreateUnitRequest;
import com.strataguard.core.dto.unit.UnitResponse;
import com.strataguard.core.dto.unit.UpdateUnitRequest;
import com.strataguard.core.entity.Unit;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.UnitMapper;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.infrastructure.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UnitService {

    private final UnitRepository unitRepository;
    private final EstateRepository estateRepository;
    private final UnitMapper unitMapper;

    public UnitResponse createUnit(CreateUnitRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify estate exists and belongs to tenant
        estateRepository.findByIdAndTenantId(request.getEstateId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", request.getEstateId()));

        // Check for duplicate unit number within estate
        if (unitRepository.existsByUnitNumberAndEstateIdAndTenantId(
                request.getUnitNumber(), request.getEstateId(), tenantId)) {
            throw new DuplicateResourceException("Unit", "unitNumber", request.getUnitNumber());
        }

        Unit unit = unitMapper.toEntity(request);
        unit.setTenantId(tenantId);

        Unit saved = unitRepository.save(unit);
        log.info("Created unit: {} in estate: {} for tenant: {}", saved.getId(), request.getEstateId(), tenantId);
        return unitMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnit(UUID unitId) {
        UUID tenantId = TenantContext.requireTenantId();
        Unit unit = unitRepository.findByIdAndTenantId(unitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", unitId));
        return unitMapper.toResponse(unit);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UnitResponse> getUnitsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify estate belongs to tenant
        estateRepository.findByIdAndTenantId(estateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", estateId));

        Page<Unit> page = unitRepository.findAllByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public UnitResponse updateUnit(UUID unitId, UpdateUnitRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Unit unit = unitRepository.findByIdAndTenantId(unitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", unitId));

        unitMapper.updateEntity(request, unit);
        Unit updated = unitRepository.save(unit);
        log.info("Updated unit: {} for tenant: {}", unitId, tenantId);
        return unitMapper.toResponse(updated);
    }

    public void deleteUnit(UUID unitId) {
        UUID tenantId = TenantContext.requireTenantId();
        Unit unit = unitRepository.findByIdAndTenantId(unitId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", unitId));

        unit.setDeleted(true);
        unit.setActive(false);
        unitRepository.save(unit);
        log.info("Soft-deleted unit: {} for tenant: {}", unitId, tenantId);
    }

    @Transactional(readOnly = true)
    public long countUnitsByEstate(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();
        return unitRepository.countByEstateIdAndTenantId(estateId, tenantId);
    }

    private PagedResponse<UnitResponse> toPagedResponse(Page<Unit> page) {
        return PagedResponse.<UnitResponse>builder()
                .content(page.getContent().stream().map(unitMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
