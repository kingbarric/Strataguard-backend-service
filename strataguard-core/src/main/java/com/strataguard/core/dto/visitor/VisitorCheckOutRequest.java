package com.strataguard.core.dto.visitor;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VisitorCheckOutRequest {

    @NotNull(message = "Visitor ID is required")
    private UUID visitorId;

    private String note;
}
