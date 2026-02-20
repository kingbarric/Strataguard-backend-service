package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.AnnouncementAudience;
import com.strataguard.core.enums.AnnouncementPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CreateAnnouncementRequest {
    @NotNull
    private UUID estateId;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    @NotNull
    private AnnouncementAudience audience;

    private String audienceFilter;
    private AnnouncementPriority priority;
    private Instant expiresAt;
    private boolean pinned;
    private boolean publishImmediately;
    private String attachmentUrl;
}
