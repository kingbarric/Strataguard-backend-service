package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Artisan;
import com.strataguard.core.entity.ArtisanRating;
import com.strataguard.core.enums.ArtisanCategory;
import com.strataguard.core.enums.ArtisanStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ArtisanMapper;
import com.strataguard.core.util.ArtisanRatingMapper;
import com.strataguard.infrastructure.repository.ArtisanRatingRepository;
import com.strataguard.infrastructure.repository.ArtisanRepository;
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
public class ArtisanService {

    private final ArtisanRepository artisanRepository;
    private final ArtisanRatingRepository ratingRepository;
    private final ArtisanMapper artisanMapper;
    private final ArtisanRatingMapper ratingMapper;

    public ArtisanResponse createArtisan(CreateArtisanRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanMapper.toEntity(request);
        artisan.setTenantId(tenantId);

        Artisan saved = artisanRepository.save(artisan);
        log.info("Created artisan: {} for tenant: {}", saved.getId(), tenantId);
        return artisanMapper.toResponse(saved);
    }

    public ArtisanResponse updateArtisan(UUID id, UpdateArtisanRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan", "id", id));

        artisanMapper.updateEntity(request, artisan);
        Artisan updated = artisanRepository.save(artisan);
        log.info("Updated artisan: {} for tenant: {}", id, tenantId);
        return artisanMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public ArtisanResponse getArtisan(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan", "id", id));

        return artisanMapper.toResponse(artisan);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ArtisanResponse> getAllArtisans(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Artisan> page = artisanRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ArtisanResponse> getArtisansByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Artisan> page = artisanRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ArtisanResponse> getArtisansByCategory(ArtisanCategory category, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Artisan> page = artisanRepository.findByCategoryAndTenantId(category, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ArtisanResponse> searchArtisans(String query, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Artisan> page = artisanRepository.searchByNameOrSpecialization(query, tenantId, pageable);
        return toPagedResponse(page);
    }

    public ArtisanResponse verifyArtisan(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan", "id", id));

        artisan.setVerified(true);
        Artisan saved = artisanRepository.save(artisan);
        log.info("Verified artisan: {} for tenant: {}", id, tenantId);
        return artisanMapper.toResponse(saved);
    }

    public ArtisanResponse updateStatus(UUID id, ArtisanStatus status) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan", "id", id));

        artisan.setStatus(status);
        Artisan saved = artisanRepository.save(artisan);
        log.info("Updated artisan {} status to {} for tenant: {}", id, status, tenantId);
        return artisanMapper.toResponse(saved);
    }

    public ArtisanRatingResponse rateArtisan(UUID artisanId, UUID residentId, RateArtisanRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanRepository.findByIdAndTenantId(artisanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan", "id", artisanId));

        // Check duplicate rating for same maintenance request
        if (request.getMaintenanceRequestId() != null) {
            boolean exists = ratingRepository.existsByArtisanIdAndResidentIdAndMaintenanceRequestId(
                    artisanId, residentId, request.getMaintenanceRequestId(), tenantId);
            if (exists) {
                throw new IllegalArgumentException("You have already rated this artisan for this job");
            }
        }

        ArtisanRating rating = new ArtisanRating();
        rating.setTenantId(tenantId);
        rating.setArtisanId(artisanId);
        rating.setResidentId(residentId);
        rating.setMaintenanceRequestId(request.getMaintenanceRequestId());
        rating.setRating(request.getRating());
        rating.setReview(request.getReview());

        ArtisanRating saved = ratingRepository.save(rating);

        // Update artisan aggregate rating
        artisan.setTotalRating(artisan.getTotalRating() + request.getRating());
        artisan.setRatingCount(artisan.getRatingCount() + 1);
        artisan.setAverageRating(artisan.getTotalRating() / artisan.getRatingCount());
        artisan.setTotalJobs(artisan.getTotalJobs() + 1);
        artisanRepository.save(artisan);

        log.info("Rated artisan: {} with {} stars for tenant: {}", artisanId, request.getRating(), tenantId);
        return ratingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ArtisanRatingResponse> getArtisanRatings(UUID artisanId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ArtisanRating> page = ratingRepository.findByArtisanIdAndTenantId(artisanId, tenantId, pageable);
        List<ArtisanRatingResponse> content = page.getContent().stream()
                .map(ratingMapper::toResponse)
                .toList();
        return PagedResponse.<ArtisanRatingResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    public void deleteArtisan(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Artisan artisan = artisanRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan", "id", id));

        artisan.setDeleted(true);
        artisanRepository.save(artisan);
        log.info("Soft-deleted artisan: {} for tenant: {}", id, tenantId);
    }

    private PagedResponse<ArtisanResponse> toPagedResponse(Page<Artisan> page) {
        List<ArtisanResponse> content = page.getContent().stream()
                .map(artisanMapper::toResponse)
                .toList();
        return PagedResponse.<ArtisanResponse>builder()
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
