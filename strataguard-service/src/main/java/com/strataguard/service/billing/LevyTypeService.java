package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.CreateLevyTypeRequest;
import com.strataguard.core.dto.billing.LevyTypeResponse;
import com.strataguard.core.dto.billing.UpdateLevyTypeRequest;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.LevyType;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.LevyTypeMapper;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.infrastructure.repository.LevyTypeRepository;
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
public class LevyTypeService {

    private final LevyTypeRepository levyTypeRepository;
    private final EstateRepository estateRepository;
    private final LevyTypeMapper levyTypeMapper;

    public LevyTypeResponse createLevyType(CreateLevyTypeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Validate estate exists
        estateRepository.findByIdAndTenantId(request.getEstateId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", request.getEstateId()));

        // Check for duplicate name within estate
        if (levyTypeRepository.existsByNameAndEstateIdAndTenantId(request.getName(), request.getEstateId(), tenantId)) {
            throw new DuplicateResourceException("LevyType", "name", request.getName());
        }

        LevyType levyType = levyTypeMapper.toEntity(request);
        levyType.setTenantId(tenantId);

        LevyType saved = levyTypeRepository.save(levyType);
        log.info("Created levy type: {} for estate: {} tenant: {}", saved.getId(), request.getEstateId(), tenantId);
        return enrichResponse(saved);
    }

    @Transactional(readOnly = true)
    public LevyTypeResponse getLevyType(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        LevyType levyType = levyTypeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LevyType", "id", id));
        return enrichResponse(levyType);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LevyTypeResponse> getAllLevyTypes(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<LevyType> page = levyTypeRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LevyTypeResponse> getLevyTypesByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<LevyType> page = levyTypeRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public LevyTypeResponse updateLevyType(UUID id, UpdateLevyTypeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        LevyType levyType = levyTypeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LevyType", "id", id));

        // Check duplicate name if name is being changed
        if (request.getName() != null && !request.getName().equals(levyType.getName())) {
            if (levyTypeRepository.existsByNameAndEstateIdAndTenantId(request.getName(), levyType.getEstateId(), tenantId)) {
                throw new DuplicateResourceException("LevyType", "name", request.getName());
            }
        }

        levyTypeMapper.updateEntity(request, levyType);
        LevyType updated = levyTypeRepository.save(levyType);
        log.info("Updated levy type: {} for tenant: {}", id, tenantId);
        return enrichResponse(updated);
    }

    public void deleteLevyType(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        LevyType levyType = levyTypeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LevyType", "id", id));

        levyType.setDeleted(true);
        levyType.setActive(false);
        levyTypeRepository.save(levyType);
        log.info("Soft-deleted levy type: {} for tenant: {}", id, tenantId);
    }

    private LevyTypeResponse enrichResponse(LevyType levyType) {
        LevyTypeResponse response = levyTypeMapper.toResponse(levyType);
        estateRepository.findByIdAndTenantId(levyType.getEstateId(), levyType.getTenantId())
                .ifPresent(estate -> response.setEstateName(estate.getName()));
        return response;
    }

    private PagedResponse<LevyTypeResponse> toPagedResponse(Page<LevyType> page) {
        return PagedResponse.<LevyTypeResponse>builder()
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
