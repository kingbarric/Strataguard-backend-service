package com.strataguard.core.dto.governance;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CastVoteRequest {
    @NotEmpty
    private List<UUID> optionIds;

    private UUID proxyForId;
}
