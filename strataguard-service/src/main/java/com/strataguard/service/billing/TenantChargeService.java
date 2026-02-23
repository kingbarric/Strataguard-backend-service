package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.TenantCharge;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.TenantChargeMapper;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.infrastructure.repository.TenancyRepository;
import com.strataguard.infrastructure.repository.TenantChargeRepository;
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
public class TenantChargeService {

    private final TenantChargeRepository tenantChargeRepository;
    private final TenancyRepository tenancyRepository;
    private final EstateRepository estateRepository;
    private final TenantChargeMapper tenantChargeMapper;

    public TenantChargeResponse createTenantCharge(CreateTenantChargeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        tenancyRepository.findByIdAndTenantId(request.getTenancyId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenancy", "id", request.getTenancyId()));

        estateRepository.findByIdAndTenantId(request.getEstateId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", request.getEstateId()));

        if (tenantChargeRepository.existsByNameAndTenancyIdAndTenantId(request.getName(), request.getTenancyId(), tenantId)) {
            throw new DuplicateResourceException("TenantCharge", "name", request.getName());
        }

        TenantCharge charge = tenantChargeMapper.toEntity(request);
        charge.setTenantId(tenantId);

        TenantCharge saved = tenantChargeRepository.save(charge);
        log.info("Created tenant charge: {} for tenancy: {} tenant: {}", saved.getId(), request.getTenancyId(), tenantId);
        return enrichResponse(saved);
    }

    @Transactional(readOnly = true)
    public TenantChargeResponse getTenantCharge(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantCharge charge = tenantChargeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantCharge", "id", id));
        return enrichResponse(charge);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantChargeResponse> getAllTenantCharges(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<TenantCharge> page = tenantChargeRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantChargeResponse> getTenantChargesByTenancy(UUID tenancyId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<TenantCharge> page = tenantChargeRepository.findByTenancyIdAndTenantId(tenancyId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantChargeResponse> getTenantChargesByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<TenantCharge> page = tenantChargeRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public TenantChargeResponse updateTenantCharge(UUID id, UpdateTenantChargeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantCharge charge = tenantChargeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantCharge", "id", id));

        if (request.getName() != null && !request.getName().equals(charge.getName())) {
            if (tenantChargeRepository.existsByNameAndTenancyIdAndTenantId(request.getName(), charge.getTenancyId(), tenantId)) {
                throw new DuplicateResourceException("TenantCharge", "name", request.getName());
            }
        }

        tenantChargeMapper.updateEntity(request, charge);
        TenantCharge updated = tenantChargeRepository.save(charge);
        log.info("Updated tenant charge: {} for tenant: {}", id, tenantId);
        return enrichResponse(updated);
    }

    public void deleteTenantCharge(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantCharge charge = tenantChargeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantCharge", "id", id));

        charge.setDeleted(true);
        charge.setActive(false);
        tenantChargeRepository.save(charge);
        log.info("Soft-deleted tenant charge: {} for tenant: {}", id, tenantId);
    }

    private TenantChargeResponse enrichResponse(TenantCharge charge) {
        TenantChargeResponse response = tenantChargeMapper.toResponse(charge);
        estateRepository.findByIdAndTenantId(charge.getEstateId(), charge.getTenantId())
                .ifPresent(estate -> response.setEstateName(estate.getName()));
        return response;
    }

    private PagedResponse<TenantChargeResponse> toPagedResponse(Page<TenantCharge> page) {
        return PagedResponse.<TenantChargeResponse>builder()
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
