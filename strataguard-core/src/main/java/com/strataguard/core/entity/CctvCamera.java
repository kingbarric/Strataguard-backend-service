package com.strataguard.core.entity;

import com.strataguard.core.enums.CameraStatus;
import com.strataguard.core.enums.CameraType;
import com.strataguard.core.enums.CameraZone;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cctv_cameras", indexes = {
        @Index(name = "idx_cctv_cameras_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_cctv_cameras_estate_id", columnList = "estate_id"),
        @Index(name = "idx_cctv_cameras_zone", columnList = "zone"),
        @Index(name = "idx_cctv_cameras_status", columnList = "status")
})
@Getter
@Setter
public class CctvCamera extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "camera_name", nullable = false)
    private String cameraName;

    @Column(name = "camera_code", nullable = false)
    private String cameraCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_type", nullable = false)
    private CameraType cameraType;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private CameraZone zone;

    @Column(name = "location")
    private String location;

    @Column(name = "stream_url")
    private String streamUrl;

    @Column(name = "ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CameraStatus status = CameraStatus.ONLINE;

    @Column(name = "last_online_at")
    private Instant lastOnlineAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
