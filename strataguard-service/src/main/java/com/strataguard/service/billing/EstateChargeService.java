package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.EstateCharge;
import com.strataguard.core.entity.EstateChargeExclusion;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.EstateChargeMapper;
import com.strataguard.infrastructure.repository.EstateChargeExclusionRepository;
import com.strataguard.infrastructure.repository.EstateChargeRepository;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.infrastructure.repository.TenancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EstateChargeService {

    private final EstateChargeRepository estateChargeRepository;
    private final EstateChargeExclusionRepository exclusionRepository;
    private final EstateRepository estateRepository;
    private final TenancyRepository tenancyRepository;
    private final EstateChargeMapper estateChargeMapper;

    public EstateChargeResponse createEstateCharge(CreateEstateChargeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        estateRepository.findByIdAndTenantId(request.getEstateId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", request.getEstateId()));

        if (estateChargeRepository.existsByNameAndEstateIdAndTenantId(request.getName(), request.getEstateId(), tenantId)) {
            throw new DuplicateResourceException("EstateCharge", "name", request.getName());
        }

        EstateCharge charge = estateChargeMapper.toEntity(request);
        charge.setTenantId(tenantId);

        EstateCharge saved = estateChargeRepository.save(charge);
        log.info("Created estate charge: {} for estate: {} tenant: {}", saved.getId(), request.getEstateId(), tenantId);
        return enrichResponse(saved);
    }

    @Transactional(readOnly = true)
    public EstateChargeResponse getEstateCharge(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        EstateCharge charge = estateChargeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateCharge", "id", id));
        return enrichResponse(charge);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EstateChargeResponse> getAllEstateCharges(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<EstateCharge> page = estateChargeRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EstateChargeResponse> getEstateChargesByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<EstateCharge> page = estateChargeRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public EstateChargeResponse updateEstateCharge(UUID id, UpdateEstateChargeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        EstateCharge charge = estateChargeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateCharge", "id", id));

        if (request.getName() != null && !request.getName().equals(charge.getName())) {
            if (estateChargeRepository.existsByNameAndEstateIdAndTenantId(request.getName(), charge.getEstateId(), tenantId)) {
                throw new DuplicateResourceException("EstateCharge", "name", request.getName());
            }
        }

        estateChargeMapper.updateEntity(request, charge);
        EstateCharge updated = estateChargeRepository.save(charge);
        log.info("Updated estate charge: {} for tenant: {}", id, tenantId);
        return enrichResponse(updated);
    }

    public void deleteEstateCharge(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        EstateCharge charge = estateChargeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateCharge", "id", id));

        charge.setDeleted(true);
        charge.setActive(false);
        estateChargeRepository.save(charge);
        log.info("Soft-deleted estate charge: {} for tenant: {}", id, tenantId);
    }

    public ExclusionResponse addExclusion(UUID chargeId, CreateExclusionRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        estateChargeRepository.findByIdAndTenantId(chargeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateCharge", "id", chargeId));

        tenancyRepository.findByIdAndTenantId(request.getTenancyId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenancy", "id", request.getTenancyId()));

        if (exclusionRepository.existsByEstateChargeIdAndTenancyIdAndTenantId(chargeId, request.getTenancyId(), tenantId)) {
            throw new DuplicateResourceException("EstateChargeExclusion", "tenancyId", request.getTenancyId());
        }

        EstateChargeExclusion exclusion = new EstateChargeExclusion();
        exclusion.setTenantId(tenantId);
        exclusion.setEstateChargeId(chargeId);
        exclusion.setTenancyId(request.getTenancyId());
        exclusion.setReason(request.getReason());

        EstateChargeExclusion saved = exclusionRepository.save(exclusion);
        log.info("Added exclusion for charge: {} tenancy: {} tenant: {}", chargeId, request.getTenancyId(), tenantId);

        return ExclusionResponse.builder()
                .id(saved.getId())
                .estateChargeId(saved.getEstateChargeId())
                .tenancyId(saved.getTenancyId())
                .reason(saved.getReason())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public void removeExclusion(UUID chargeId, UUID exclusionId) {
        UUID tenantId = TenantContext.requireTenantId();

        EstateChargeExclusion exclusion = exclusionRepository.findByIdAndTenantId(exclusionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateChargeExclusion", "id", exclusionId));

        if (!exclusion.getEstateChargeId().equals(chargeId)) {
            throw new ResourceNotFoundException("EstateChargeExclusion", "id", exclusionId);
        }

        exclusion.setDeleted(true);
        exclusionRepository.save(exclusion);
        log.info("Removed exclusion: {} for charge: {} tenant: {}", exclusionId, chargeId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<ExclusionResponse> getExclusions(UUID chargeId) {
        UUID tenantId = TenantContext.requireTenantId();

        estateChargeRepository.findByIdAndTenantId(chargeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateCharge", "id", chargeId));

        return exclusionRepository.findByEstateChargeIdAndTenantId(chargeId, tenantId).stream()
                .map(e -> ExclusionResponse.builder()
                        .id(e.getId())
                        .estateChargeId(e.getEstateChargeId())
                        .tenancyId(e.getTenancyId())
                        .reason(e.getReason())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }

    private EstateChargeResponse enrichResponse(EstateCharge charge) {
        EstateChargeResponse response = estateChargeMapper.toResponse(charge);
        estateRepository.findByIdAndTenantId(charge.getEstateId(), charge.getTenantId())
                .ifPresent(estate -> response.setEstateName(estate.getName()));
        return response;
    }

    private PagedResponse<EstateChargeResponse> toPagedResponse(Page<EstateCharge> page) {
        return PagedResponse.<EstateChargeResponse>builder()
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
