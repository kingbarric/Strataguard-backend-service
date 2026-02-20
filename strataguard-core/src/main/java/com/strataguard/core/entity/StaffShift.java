package com.strataguard.core.entity;

import com.strataguard.core.enums.ShiftType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "staff_shifts", indexes = {
        @Index(name = "idx_staff_shifts_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_staff_shifts_staff_id", columnList = "staff_id"),
        @Index(name = "idx_staff_shifts_estate_id", columnList = "estate_id")
})
@Getter
@Setter
public class StaffShift extends BaseEntity {

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftType shiftType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
