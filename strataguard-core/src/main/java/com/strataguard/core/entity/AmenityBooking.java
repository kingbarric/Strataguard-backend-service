package com.strataguard.core.entity;

import com.strataguard.core.enums.BookingStatus;
import com.strataguard.core.enums.RecurrencePattern;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "amenity_bookings", indexes = {
        @Index(name = "idx_amenity_bookings_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_amenity_bookings_amenity_id", columnList = "amenity_id"),
        @Index(name = "idx_amenity_bookings_resident_id", columnList = "resident_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AmenityBooking extends BaseEntity {

    @Column(name = "booking_reference", nullable = false, unique = true)
    private String bookingReference;

    @Column(name = "amenity_id", nullable = false)
    private UUID amenityId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "amount_charged", precision = 15, scale = 2)
    private BigDecimal amountCharged;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "number_of_guests")
    private Integer numberOfGuests;

    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean recurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_pattern")
    private RecurrencePattern recurrencePattern;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "parent_booking_id")
    private UUID parentBookingId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;
}
