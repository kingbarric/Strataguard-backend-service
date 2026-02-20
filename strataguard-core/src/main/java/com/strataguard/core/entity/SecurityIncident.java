package com.strataguard.core.entity;

import com.strataguard.core.enums.IncidentCategory;
import com.strataguard.core.enums.IncidentSeverity;
import com.strataguard.core.enums.IncidentStatus;
import com.strataguard.core.enums.ReporterType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "security_incidents", indexes = {
        @Index(name = "idx_security_incidents_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_security_incidents_estate_id", columnList = "estate_id"),
        @Index(name = "idx_security_incidents_status", columnList = "status"),
        @Index(name = "idx_security_incidents_severity", columnList = "severity"),
        @Index(name = "idx_security_incidents_category", columnList = "category")
})
@Getter
@Setter
public class SecurityIncident extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "incident_number", nullable = false)
    private String incidentNumber;

    @Column(name = "reported_by", nullable = false)
    private UUID reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false)
    private ReporterType reporterType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IncidentStatus status = IncidentStatus.REPORTED;

    @Column(name = "location")
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private List<String> photoUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "witnesses", columnDefinition = "jsonb")
    private List<String> witnesses;

    @Column(name = "linked_alert_id")
    private UUID linkedAlertId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "closed_at")
    private Instant closedAt;
}
