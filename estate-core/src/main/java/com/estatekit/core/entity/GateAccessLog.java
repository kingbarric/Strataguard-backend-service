package com.estatekit.core.entity;

import com.estatekit.core.enums.GateEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "gate_access_logs", indexes = {
        @Index(name = "idx_gate_access_logs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_gate_access_logs_session_id", columnList = "session_id"),
        @Index(name = "idx_gate_access_logs_vehicle_id", columnList = "vehicle_id")
})
@Getter
@Setter
@NoArgsConstructor
public class GateAccessLog extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private GateEventType eventType;

    @Column(name = "guard_id", nullable = false)
    private String guardId;

    @Column(name = "details")
    private String details;

    @Column(nullable = false)
    private boolean success = true;
}
