package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_waitlist", indexes = {
        @Index(name = "idx_booking_waitlist_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_booking_waitlist_amenity_id", columnList = "amenity_id")
})
@Getter
@Setter
@NoArgsConstructor
public class BookingWaitlist extends BaseEntity {

    @Column(name = "amenity_id", nullable = false)
    private UUID amenityId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "desired_start_time", nullable = false)
    private Instant desiredStartTime;

    @Column(name = "desired_end_time", nullable = false)
    private Instant desiredEndTime;

    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "notified_at")
    private Instant notifiedAt;
}
