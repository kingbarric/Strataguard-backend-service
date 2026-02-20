package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.AnnouncementAudience;
import com.strataguard.core.enums.AnnouncementPriority;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UpdateAnnouncementRequest {
    private String title;
    private String body;
    private AnnouncementAudience audience;
    private String audienceFilter;
    private AnnouncementPriority priority;
    private Instant expiresAt;
    private Boolean pinned;
    private String attachmentUrl;
}
