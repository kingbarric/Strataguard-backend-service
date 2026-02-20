package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.PollStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PollResponse {
    private UUID id;
    private UUID estateId;
    private String title;
    private String description;
    private PollStatus status;
    private String createdByName;
    private Instant startsAt;
    private Instant deadline;
    private boolean allowMultipleChoices;
    private boolean anonymous;
    private boolean allowProxyVoting;
    private int totalVotes;
    private Integer eligibleVoterCount;
    private List<PollOptionResponse> options;
    private Instant createdAt;
    private Instant updatedAt;
}
