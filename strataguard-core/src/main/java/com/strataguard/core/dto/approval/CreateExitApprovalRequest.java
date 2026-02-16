package com.strataguard.core.dto.approval;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateExitApprovalRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    private String note;
}
