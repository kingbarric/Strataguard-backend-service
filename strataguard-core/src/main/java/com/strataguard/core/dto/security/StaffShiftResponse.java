package com.strataguard.core.dto.security;

import com.strataguard.core.enums.ShiftType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class StaffShiftResponse {

    private UUID id;
    private UUID staffId;
    private UUID estateId;
    private ShiftType shiftType;
    private LocalTime startTime;
    private LocalTime endTime;
    private String daysOfWeek;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
