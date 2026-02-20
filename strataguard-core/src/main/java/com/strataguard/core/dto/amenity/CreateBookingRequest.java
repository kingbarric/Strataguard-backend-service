package com.strataguard.core.dto.amenity;

import com.strataguard.core.enums.RecurrencePattern;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateBookingRequest {
    @NotNull(message = "Amenity ID is required")
    private UUID amenityId;
    @NotNull(message = "Start time is required")
    private Instant startTime;
    @NotNull(message = "End time is required")
    private Instant endTime;
    private Integer numberOfGuests;
    private String purpose;
    private String notes;
    private boolean recurring;
    private RecurrencePattern recurrencePattern;
    private LocalDate recurrenceEndDate;
}
