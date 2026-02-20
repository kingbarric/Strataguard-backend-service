package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.BookingWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingWaitlistRepository extends JpaRepository<BookingWaitlist, UUID> {

    @Query("SELECT w FROM BookingWaitlist w WHERE w.amenityId = :amenityId AND w.tenantId = :tenantId " +
            "AND w.notified = false AND w.desiredStartTime < :endTime AND w.desiredEndTime > :startTime AND w.deleted = false " +
            "ORDER BY w.createdAt ASC")
    List<BookingWaitlist> findMatchingWaitlistEntries(@Param("amenityId") UUID amenityId,
                                                      @Param("startTime") Instant startTime,
                                                      @Param("endTime") Instant endTime,
                                                      @Param("tenantId") UUID tenantId);
}
