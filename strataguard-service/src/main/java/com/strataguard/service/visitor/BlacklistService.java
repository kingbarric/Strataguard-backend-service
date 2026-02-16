package com.strataguard.service.visitor;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.blacklist.BlacklistResponse;
import com.strataguard.core.dto.blacklist.CreateBlacklistRequest;
import com.strataguard.core.dto.blacklist.UpdateBlacklistRequest;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Blacklist;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.BlacklistMapper;
import com.strataguard.core.util.PlateNumberUtils;
import com.strataguard.infrastructure.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final BlacklistMapper blacklistMapper;

    public BlacklistResponse create(CreateBlacklistRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.getName() == null && request.getPhone() == null && request.getPlateNumber() == null) {
            throw new IllegalArgumentException("At least one of name, phone, or plate number is required");
        }

        Blacklist blacklist = blacklistMapper.toEntity(request);
        blacklist.setTenantId(tenantId);
        blacklist.setAddedBy(currentUser);

        if (request.getPlateNumber() != null) {
            blacklist.setPlateNumber(PlateNumberUtils.normalize(request.getPlateNumber()));
        }

        Blacklist saved = blacklistRepository.save(blacklist);
        log.info("Created blacklist entry: {} for tenant: {}", saved.getId(), tenantId);
        return blacklistMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BlacklistResponse getById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Blacklist blacklist = blacklistRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Blacklist", "id", id));
        return blacklistMapper.toResponse(blacklist);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlacklistResponse> getAll(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Blacklist> page = blacklistRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    public BlacklistResponse update(UUID id, UpdateBlacklistRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Blacklist blacklist = blacklistRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Blacklist", "id", id));

        if (request.getPlateNumber() != null) {
            request.setPlateNumber(PlateNumberUtils.normalize(request.getPlateNumber()));
        }

        blacklistMapper.updateEntity(request, blacklist);
        Blacklist updated = blacklistRepository.save(blacklist);
        log.info("Updated blacklist entry: {} for tenant: {}", id, tenantId);
        return blacklistMapper.toResponse(updated);
    }

    public void deactivate(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Blacklist blacklist = blacklistRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Blacklist", "id", id));

        blacklist.setActive(false);
        blacklistRepository.save(blacklist);
        log.info("Deactivated blacklist entry: {} for tenant: {}", id, tenantId);
    }

    public void delete(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Blacklist blacklist = blacklistRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Blacklist", "id", id));

        blacklist.setDeleted(true);
        blacklistRepository.save(blacklist);
        log.info("Soft-deleted blacklist entry: {} for tenant: {}", id, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlacklistResponse> search(String query, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Blacklist> page = blacklistRepository.search(query, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public boolean isPlateBlacklisted(String plateNumber) {
        UUID tenantId = TenantContext.requireTenantId();
        String normalized = PlateNumberUtils.normalize(plateNumber);
        return blacklistRepository.isPlateBlacklisted(normalized, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isPhoneBlacklisted(String phone) {
        UUID tenantId = TenantContext.requireTenantId();
        return blacklistRepository.isPhoneBlacklisted(phone, tenantId);
    }

    private PagedResponse<BlacklistResponse> toPagedResponse(Page<Blacklist> page) {
        return PagedResponse.<BlacklistResponse>builder()
                .content(page.getContent().stream().map(blacklistMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
