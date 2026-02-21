package com.strataguard.core.entity;

import com.strataguard.core.enums.MaintenanceCategory;
import com.strataguard.core.enums.MaintenancePriority;
import com.strataguard.core.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_requests", indexes = {
        @Index(name = "idx_maintenance_requests_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_maintenance_requests_unit_id", columnList = "unit_id"),
        @Index(name = "idx_maintenance_requests_estate_id", columnList = "estate_id"),
        @Index(name = "idx_maintenance_requests_resident_id", columnList = "resident_id"),
        @Index(name = "idx_maintenance_requests_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRequest extends BaseEntity {

    @Column(name = "request_number", nullable = false, unique = true)
    private String requestNumber;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "assigned_to_phone")
    private String assignedToPhone;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private String photoUrls;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 15, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "cost_approved_by")
    private String costApprovedBy;

    @Column(name = "cost_approved_at")
    private Instant costApprovedAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached = false;

    @Column(nullable = false)
    private boolean escalated = false;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    @Column(name = "satisfaction_comment", columnDefinition = "TEXT")
    private String satisfactionComment;
}
