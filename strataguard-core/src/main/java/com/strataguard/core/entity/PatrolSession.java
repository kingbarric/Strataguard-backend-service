package com.strataguard.core.entity;

import com.strataguard.core.enums.PatrolSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patrol_sessions", indexes = {
        @Index(name = "idx_patrol_sessions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_patrol_sessions_staff_id", columnList = "staff_id"),
        @Index(name = "idx_patrol_sessions_estate_id", columnList = "estate_id"),
        @Index(name = "idx_patrol_sessions_status", columnList = "status")
})
@Getter
@Setter
public class PatrolSession extends BaseEntity {

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PatrolSessionStatus status = PatrolSessionStatus.IN_PROGRESS;

    @Column(name = "total_checkpoints")
    private int totalCheckpoints;

    @Column(name = "scanned_checkpoints")
    private int scannedCheckpoints;

    @Column(name = "completion_percentage")
    private Double completionPercentage;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
