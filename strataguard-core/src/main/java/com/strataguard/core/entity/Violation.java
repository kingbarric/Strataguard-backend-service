package com.strataguard.core.entity;

import com.strataguard.core.enums.ViolationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "violations", indexes = {
        @Index(name = "idx_violation_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_violation_estate_id", columnList = "estate_id"),
        @Index(name = "idx_violation_unit_id", columnList = "unit_id"),
        @Index(name = "idx_violation_status", columnList = "status")
})
@Getter
@Setter
public class Violation extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "rule_violated", nullable = false)
    private String ruleViolated;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "fine_amount")
    private BigDecimal fineAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ViolationStatus status = ViolationStatus.REPORTED;

    @Column(name = "reported_by", nullable = false)
    private String reportedBy;

    @Column(name = "reported_by_name")
    private String reportedByName;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "appeal_reason", columnDefinition = "TEXT")
    private String appealReason;

    @Column(name = "appealed_at")
    private Instant appealedAt;
}
