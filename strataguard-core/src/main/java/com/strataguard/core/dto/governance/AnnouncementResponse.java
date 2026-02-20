package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.AnnouncementAudience;
import com.strataguard.core.enums.AnnouncementPriority;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AnnouncementResponse {
    private UUID id;
    private UUID estateId;
    private String title;
    private String body;
    private AnnouncementAudience audience;
    private String audienceFilter;
    private AnnouncementPriority priority;
    private String postedBy;
    private String postedByName;
    private Instant publishedAt;
    private Instant expiresAt;
    private boolean pinned;
    private boolean published;
    private String attachmentUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
