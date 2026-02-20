package com.strataguard.core.dto.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreatePollRequest {
    @NotNull
    private UUID estateId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Instant deadline;

    private Instant startsAt;
    private boolean allowMultipleChoices;
    private boolean anonymous;
    private boolean allowProxyVoting;
    private Integer eligibleVoterCount;

    @NotEmpty
    private List<String> options;
}
