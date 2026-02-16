package com.strataguard.service.estate;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.estate.CreateEstateRequest;
import com.strataguard.core.dto.estate.EstateResponse;
import com.strataguard.core.dto.estate.UpdateEstateRequest;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.EstateMapper;
import com.strataguard.infrastructure.repository.EstateRepository;
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
public class EstateService {

    private final EstateRepository estateRepository;
    private final EstateMapper estateMapper;

    public EstateResponse createEstate(CreateEstateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (estateRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new DuplicateResourceException("Estate", "name", request.getName());
        }

        Estate estate = estateMapper.toEntity(request);
        estate.setTenantId(tenantId);

        Estate saved = estateRepository.save(estate);
        log.info("Created estate: {} for tenant: {}", saved.getId(), tenantId);
        return estateMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EstateResponse getEstate(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();
        Estate estate = estateRepository.findByIdAndTenantId(estateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", estateId));
        return estateMapper.toResponse(estate);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EstateResponse> getAllEstates(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Estate> page = estateRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EstateResponse> searchEstates(String search, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Estate> page = estateRepository.searchByTenantId(tenantId, search, pageable);
        return toPagedResponse(page);
    }

    public EstateResponse updateEstate(UUID estateId, UpdateEstateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Estate estate = estateRepository.findByIdAndTenantId(estateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", estateId));

        estateMapper.updateEntity(request, estate);
        Estate updated = estateRepository.save(estate);
        log.info("Updated estate: {} for tenant: {}", estateId, tenantId);
        return estateMapper.toResponse(updated);
    }

    public void deleteEstate(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();
        Estate estate = estateRepository.findByIdAndTenantId(estateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", estateId));

        estate.setDeleted(true);
        estate.setActive(false);
        estateRepository.save(estate);
        log.info("Soft-deleted estate: {} for tenant: {}", estateId, tenantId);
    }

    private PagedResponse<EstateResponse> toPagedResponse(Page<Estate> page) {
        return PagedResponse.<EstateResponse>builder()
                .content(page.getContent().stream().map(estateMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
