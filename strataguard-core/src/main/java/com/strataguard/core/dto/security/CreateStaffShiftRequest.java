package com.strataguard.core.dto.security;

import com.strataguard.core.enums.ShiftType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class CreateStaffShiftRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotNull(message = "Shift type is required")
    private ShiftType shiftType;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private String daysOfWeek;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
