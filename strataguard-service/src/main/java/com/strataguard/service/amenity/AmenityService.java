package com.strataguard.service.amenity;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.amenity.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Amenity;
import com.strataguard.core.entity.AmenityBooking;
import com.strataguard.core.enums.AmenityStatus;
import com.strataguard.core.enums.BookingStatus;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.AmenityMapper;
import com.strataguard.infrastructure.repository.AmenityBookingRepository;
import com.strataguard.infrastructure.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final AmenityBookingRepository bookingRepository;
    private final AmenityMapper amenityMapper;

    public AmenityResponse createAmenity(CreateAmenityRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (amenityRepository.existsByNameAndEstateIdAndTenantId(request.getName(), request.getEstateId(), tenantId)) {
            throw new DuplicateResourceException("Amenity", "name", request.getName());
        }

        Amenity amenity = amenityMapper.toEntity(request);
        amenity.setTenantId(tenantId);

        Amenity saved = amenityRepository.save(amenity);
        log.info("Created amenity: {} for estate: {} tenant: {}", saved.getId(), request.getEstateId(), tenantId);
        return amenityMapper.toResponse(saved);
    }

    public AmenityResponse updateAmenity(UUID amenityId, UpdateAmenityRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Amenity amenity = amenityRepository.findByIdAndTenantId(amenityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));

        amenityMapper.updateEntity(request, amenity);
        Amenity updated = amenityRepository.save(amenity);
        log.info("Updated amenity: {} for tenant: {}", amenityId, tenantId);
        return amenityMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public AmenityResponse getAmenity(UUID amenityId) {
        UUID tenantId = TenantContext.requireTenantId();
        Amenity amenity = amenityRepository.findByIdAndTenantId(amenityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));
        return amenityMapper.toResponse(amenity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AmenityResponse> getAllAmenities(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Amenity> page = amenityRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AmenityResponse> getAmenitiesByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Amenity> page = amenityRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public AmenityResponse updateAmenityStatus(UUID amenityId, AmenityStatus newStatus) {
        UUID tenantId = TenantContext.requireTenantId();
        Amenity amenity = amenityRepository.findByIdAndTenantId(amenityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));

        AmenityStatus oldStatus = amenity.getStatus();
        amenity.setStatus(newStatus);

        // When put under maintenance, cancel all future bookings
        if (newStatus == AmenityStatus.UNDER_MAINTENANCE && oldStatus != AmenityStatus.UNDER_MAINTENANCE) {
            cancelFutureBookings(amenityId, tenantId, "Amenity placed under maintenance");
        }

        Amenity saved = amenityRepository.save(amenity);
        log.info("Updated amenity {} status from {} to {} for tenant: {}", amenityId, oldStatus, newStatus, tenantId);
        return amenityMapper.toResponse(saved);
    }

    public void deleteAmenity(UUID amenityId) {
        UUID tenantId = TenantContext.requireTenantId();
        Amenity amenity = amenityRepository.findByIdAndTenantId(amenityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));

        cancelFutureBookings(amenityId, tenantId, "Amenity deleted");
        amenity.setDeleted(true);
        amenity.setActive(false);
        amenityRepository.save(amenity);
        log.info("Soft-deleted amenity: {} for tenant: {}", amenityId, tenantId);
    }

    @Transactional(readOnly = true)
    public AmenityDashboardResponse getDashboard() {
        UUID tenantId = TenantContext.requireTenantId();

        Instant dayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return AmenityDashboardResponse.builder()
                .totalAmenities(amenityRepository.countByTenantId(tenantId))
                .activeAmenities(amenityRepository.countByStatusAndTenantId(AmenityStatus.ACTIVE, tenantId))
                .totalBookingsToday(bookingRepository.countTodayBookings(tenantId, dayStart, dayEnd))
                .pendingBookings(bookingRepository.countByStatusAndTenantId(BookingStatus.PENDING, tenantId))
                .confirmedBookings(bookingRepository.countByStatusAndTenantId(BookingStatus.CONFIRMED, tenantId))
                .build();
    }

    private void cancelFutureBookings(UUID amenityId, UUID tenantId, String reason) {
        Instant now = Instant.now();
        List<AmenityBooking> futureBookings = bookingRepository.findUpcomingConfirmedBookings(
                tenantId, now, now.plusSeconds(365L * 24 * 3600));
        for (AmenityBooking booking : futureBookings) {
            if (booking.getAmenityId().equals(amenityId)) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(now);
                booking.setCancellationReason(reason);
                bookingRepository.save(booking);
            }
        }
    }

    private PagedResponse<AmenityResponse> toPagedResponse(Page<Amenity> page) {
        return PagedResponse.<AmenityResponse>builder()
                .content(page.getContent().stream().map(amenityMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
