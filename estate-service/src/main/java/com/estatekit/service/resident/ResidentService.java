package com.estatekit.service.resident;

import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.resident.*;
import com.estatekit.core.entity.Resident;
import com.estatekit.core.exception.DuplicateResourceException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.ResidentMapper;
import com.estatekit.core.config.TenantContext;
import com.estatekit.infrastructure.repository.ResidentRepository;
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
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final ResidentMapper residentMapper;

    public ResidentResponse createResident(CreateResidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (request.getEmail() != null && residentRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new DuplicateResourceException("Resident", "email", request.getEmail());
        }

        Resident resident = residentMapper.toEntity(request);
        resident.setTenantId(tenantId);

        Resident saved = residentRepository.save(resident);
        log.info("Created resident: {} for tenant: {}", saved.getId(), tenantId);
        return residentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResidentResponse getResident(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", residentId));
        return residentMapper.toResponse(resident);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResidentResponse> getAllResidents(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Resident> page = residentRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResidentResponse> searchResidents(String search, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Resident> page = residentRepository.searchByTenantId(tenantId, search, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public ResidentResponse getByUserId(String userId) {
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId));
        return residentMapper.toResponse(resident);
    }

    public ResidentResponse updateResident(UUID residentId, UpdateResidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", residentId));

        residentMapper.updateEntity(request, resident);
        Resident updated = residentRepository.save(resident);
        log.info("Updated resident: {} for tenant: {}", residentId, tenantId);
        return residentMapper.toResponse(updated);
    }

    public void deleteResident(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", residentId));

        resident.setDeleted(true);
        resident.setActive(false);
        residentRepository.save(resident);
        log.info("Soft-deleted resident: {} for tenant: {}", residentId, tenantId);
    }

    public ResidentResponse linkKeycloakUser(UUID residentId, LinkKeycloakUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", residentId));

        if (residentRepository.existsByUserIdAndTenantId(request.getUserId(), tenantId)) {
            throw new DuplicateResourceException("Resident", "userId", request.getUserId());
        }

        resident.setUserId(request.getUserId());
        Resident updated = residentRepository.save(resident);
        log.info("Linked Keycloak user {} to resident {} for tenant: {}", request.getUserId(), residentId, tenantId);
        return residentMapper.toResponse(updated);
    }

    private PagedResponse<ResidentResponse> toPagedResponse(Page<Resident> page) {
        return PagedResponse.<ResidentResponse>builder()
                .content(page.getContent().stream().map(residentMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
