package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.LevyFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LevyTypeResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal amount;
    private LevyFrequency frequency;
    private UUID estateId;
    private String estateName;
    private String category;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
