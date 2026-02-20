package com.strataguard.core.entity;

import com.strataguard.core.enums.EmergencyAlertStatus;
import com.strataguard.core.enums.EmergencyAlertType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_alerts", indexes = {
        @Index(name = "idx_emergency_alerts_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_emergency_alerts_estate_id", columnList = "estate_id"),
        @Index(name = "idx_emergency_alerts_status", columnList = "status"),
        @Index(name = "idx_emergency_alerts_resident_id", columnList = "resident_id")
})
@Getter
@Setter
public class EmergencyAlert extends BaseEntity {

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private EmergencyAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmergencyAlertStatus status = EmergencyAlertStatus.TRIGGERED;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "responded_by")
    private UUID respondedBy;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "response_time_seconds")
    private Long responseTimeSeconds;
}
