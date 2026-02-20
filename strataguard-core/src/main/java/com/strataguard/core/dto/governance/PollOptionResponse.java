package com.strataguard.core.dto.governance;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PollOptionResponse {
    private UUID id;
    private String optionText;
    private int voteCount;
    private int displayOrder;
    private double percentage;
}
