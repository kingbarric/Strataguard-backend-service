package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.AmenityBooking;
import com.strataguard.core.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityBookingRepository extends JpaRepository<AmenityBooking, UUID> {

    @Query("SELECT b FROM AmenityBooking b WHERE b.id = :id AND b.tenantId = :tenantId AND b.deleted = false")
    Optional<AmenityBooking> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM AmenityBooking b WHERE b.amenityId = :amenityId AND b.tenantId = :tenantId " +
            "AND b.status IN ('CONFIRMED', 'PENDING') AND b.startTime < :endTime AND b.endTime > :startTime AND b.deleted = false")
    List<AmenityBooking> findOverlappingBookings(@Param("amenityId") UUID amenityId,
                                                  @Param("startTime") Instant startTime,
                                                  @Param("endTime") Instant endTime,
                                                  @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM AmenityBooking b WHERE b.residentId = :residentId AND b.tenantId = :tenantId AND b.deleted = false")
    Page<AmenityBooking> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT b FROM AmenityBooking b WHERE b.amenityId = :amenityId AND b.tenantId = :tenantId " +
            "AND b.startTime >= :dayStart AND b.startTime < :dayEnd AND b.status IN ('CONFIRMED', 'PENDING') AND b.deleted = false")
    List<AmenityBooking> findByAmenityIdAndDateAndTenantId(@Param("amenityId") UUID amenityId,
                                                            @Param("dayStart") Instant dayStart,
                                                            @Param("dayEnd") Instant dayEnd,
                                                            @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM AmenityBooking b WHERE b.amenityId = :amenityId AND b.tenantId = :tenantId AND b.deleted = false")
    Page<AmenityBooking> findByAmenityIdAndTenantId(@Param("amenityId") UUID amenityId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT b FROM AmenityBooking b WHERE b.status = 'CONFIRMED' AND b.tenantId = :tenantId " +
            "AND b.startTime BETWEEN :from AND :to AND b.deleted = false")
    List<AmenityBooking> findUpcomingConfirmedBookings(@Param("tenantId") UUID tenantId,
                                                       @Param("from") Instant from,
                                                       @Param("to") Instant to);

    @Query("SELECT b FROM AmenityBooking b WHERE b.status = 'CONFIRMED' AND b.endTime < :now AND b.tenantId = :tenantId AND b.deleted = false")
    List<AmenityBooking> findCompletableBookings(@Param("now") Instant now, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(b) FROM AmenityBooking b WHERE b.tenantId = :tenantId AND b.status = :status AND b.deleted = false")
    long countByStatusAndTenantId(@Param("status") BookingStatus status, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(b) FROM AmenityBooking b WHERE b.tenantId = :tenantId " +
            "AND b.startTime >= :dayStart AND b.startTime < :dayEnd AND b.deleted = false")
    long countTodayBookings(@Param("tenantId") UUID tenantId, @Param("dayStart") Instant dayStart, @Param("dayEnd") Instant dayEnd);
}
