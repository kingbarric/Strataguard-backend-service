package com.strataguard.core.dto.maintenance;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID requestId;
    private UUID authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private String attachmentUrls;
    private boolean internal;
    private Instant createdAt;
}
