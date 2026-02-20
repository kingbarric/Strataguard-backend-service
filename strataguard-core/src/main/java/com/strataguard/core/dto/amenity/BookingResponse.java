package com.strataguard.core.dto.amenity;

import com.strataguard.core.enums.BookingStatus;
import com.strataguard.core.enums.RecurrencePattern;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private String bookingReference;
    private UUID amenityId;
    private String amenityName;
    private UUID residentId;
    private String residentName;
    private Instant startTime;
    private Instant endTime;
    private BookingStatus status;
    private BigDecimal amountCharged;
    private BigDecimal amountPaid;
    private String paymentReference;
    private Integer numberOfGuests;
    private String purpose;
    private String notes;
    private boolean recurring;
    private RecurrencePattern recurrencePattern;
    private LocalDate recurrenceEndDate;
    private UUID parentBookingId;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant createdAt;
}
