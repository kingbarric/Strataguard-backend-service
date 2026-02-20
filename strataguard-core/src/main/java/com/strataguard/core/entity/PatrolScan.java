package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patrol_scans", indexes = {
        @Index(name = "idx_patrol_scans_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_patrol_scans_session_id", columnList = "session_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_patrol_scan_session_checkpoint",
                columnNames = {"session_id", "checkpoint_id"})
})
@Getter
@Setter
public class PatrolScan extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "checkpoint_id", nullable = false)
    private UUID checkpointId;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "photo_url")
    private String photoUrl;
}
