package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.ComplaintCategory;
import com.strataguard.core.enums.ComplaintStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ComplaintResponse {
    private UUID id;
    private UUID estateId;
    private UUID residentId;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private boolean anonymous;
    private String assignedTo;
    private String assignedToName;
    private String responseNotes;
    private Instant resolvedAt;
    private String attachmentUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
