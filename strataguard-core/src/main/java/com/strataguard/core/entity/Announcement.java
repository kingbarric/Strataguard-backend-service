package com.strataguard.core.entity;

import com.strataguard.core.enums.AnnouncementAudience;
import com.strataguard.core.enums.AnnouncementPriority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_announcement_estate_id", columnList = "estate_id"),
        @Index(name = "idx_announcement_audience", columnList = "audience"),
        @Index(name = "idx_announcement_published_at", columnList = "published_at")
})
@Getter
@Setter
public class Announcement extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false)
    private AnnouncementAudience audience;

    @Column(name = "audience_filter")
    private String audienceFilter;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @Column(name = "posted_by", nullable = false)
    private String postedBy;

    @Column(name = "posted_by_name")
    private String postedByName;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "is_published", nullable = false)
    private boolean published = false;

    @Column(name = "attachment_url")
    private String attachmentUrl;
}