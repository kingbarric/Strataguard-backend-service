package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.LevyFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstateChargeResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal amount;
    private LevyFrequency frequency;
    private UUID estateId;
    private String estateName;
    private String category;
    private List<Integer> reminderDaysBefore;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
