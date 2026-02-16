package com.strataguard.core.entity;

import com.strataguard.core.enums.GateSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gate_sessions", indexes = {
        @Index(name = "idx_gate_sessions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_gate_sessions_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_gate_sessions_resident_id", columnList = "resident_id"),
        @Index(name = "idx_gate_sessions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class GateSession extends BaseEntity {

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "plate_number", nullable = false)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GateSessionStatus status = GateSessionStatus.OPEN;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @Column(name = "entry_guard_id", nullable = false)
    private String entryGuardId;

    @Column(name = "exit_guard_id")
    private String exitGuardId;

    @Column(name = "entry_note")
    private String entryNote;

    @Column(name = "exit_note")
    private String exitNote;
}
