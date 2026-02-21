package com.strataguard.core.entity;

import com.strataguard.core.enums.ComplaintCategory;
import com.strataguard.core.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaint_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_complaint_estate_id", columnList = "estate_id"),
        @Index(name = "idx_complaint_resident_id", columnList = "resident_id"),
        @Index(name = "idx_complaint_status", columnList = "status")
})
@Getter
@Setter
public class Complaint extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "attachment_url")
    private String attachmentUrl;
}
