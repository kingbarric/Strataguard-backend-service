package com.strataguard.core.dto.amenity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AvailabilityResponse {
    private LocalDate date;
    private List<TimeSlot> availableSlots;
    private List<TimeSlot> bookedSlots;

    @Data
    @Builder
    public static class TimeSlot {
        private Instant startTime;
        private Instant endTime;
    }
}
