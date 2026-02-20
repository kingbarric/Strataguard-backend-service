package com.strataguard.core.entity;

import com.strataguard.core.enums.CameraStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "camera_status_logs", indexes = {
        @Index(name = "idx_camera_status_logs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_camera_status_logs_camera_id", columnList = "camera_id")
})
@Getter
@Setter
public class CameraStatusLog extends BaseEntity {

    @Column(name = "camera_id", nullable = false)
    private UUID cameraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private CameraStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private CameraStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "reason")
    private String reason;
}
