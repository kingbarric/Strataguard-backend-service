package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Announcement;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.AnnouncementMapper;
import com.strataguard.infrastructure.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request, String userId, String userName) {
        UUID tenantId = TenantContext.requireTenantId();

        Announcement announcement = announcementMapper.toEntity(request);
        announcement.setTenantId(tenantId);
        announcement.setPostedBy(userId);
        announcement.setPostedByName(userName);

        if (request.isPublishImmediately()) {
            announcement.setPublished(true);
            announcement.setPublishedAt(Instant.now());
        }

        Announcement saved = announcementRepository.save(announcement);
        log.info("Created announcement: {} for tenant: {}", saved.getId(), tenantId);
        return announcementMapper.toResponse(saved);
    }

    public AnnouncementResponse updateAnnouncement(UUID id, UpdateAnnouncementRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Announcement announcement = announcementRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcementMapper.updateEntity(request, announcement);
        if (request.getPinned() != null) {
            announcement.setPinned(request.getPinned());
        }

        Announcement updated = announcementRepository.save(announcement);
        log.info("Updated announcement: {} for tenant: {}", id, tenantId);
        return announcementMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncement(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Announcement announcement = announcementRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        return announcementMapper.toResponse(announcement);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Announcement> page = announcementRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> getAnnouncementsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Announcement> page = announcementRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> getActiveAnnouncements(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Announcement> page = announcementRepository.findActiveByEstateId(estateId, tenantId, Instant.now(), pageable);
        return toPagedResponse(page);
    }

    public AnnouncementResponse publishAnnouncement(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Announcement announcement = announcementRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (announcement.isPublished()) {
            throw new IllegalStateException("Announcement is already published");
        }

        announcement.setPublished(true);
        announcement.setPublishedAt(Instant.now());
        Announcement saved = announcementRepository.save(announcement);
        log.info("Published announcement: {} for tenant: {}", id, tenantId);
        return announcementMapper.toResponse(saved);
    }

    public AnnouncementResponse unpublishAnnouncement(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Announcement announcement = announcementRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcement.setPublished(false);
        Announcement saved = announcementRepository.save(announcement);
        log.info("Unpublished announcement: {} for tenant: {}", id, tenantId);
        return announcementMapper.toResponse(saved);
    }

    public void deleteAnnouncement(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Announcement announcement = announcementRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcement.setDeleted(true);
        announcementRepository.save(announcement);
        log.info("Soft-deleted announcement: {} for tenant: {}", id, tenantId);
    }

    private PagedResponse<AnnouncementResponse> toPagedResponse(Page<Announcement> page) {
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(announcementMapper::toResponse)
                .toList();
        return PagedResponse.<AnnouncementResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
